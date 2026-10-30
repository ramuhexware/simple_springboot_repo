package com.hexaware.loanapp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    // [FUNCTIONAL FLOW: Single Loan Application Evaluation and Underwriting]
    @Transactional
    public Loan evaluateAndProcessLoan(Loan loan) {
        // [DECISION NODE: Input Parameter Validation]
        if (loan == null ||
            loan.getRequestedAmount() == null || loan.getRequestedAmount() <= 0.0 ||
            loan.getAnnualIncome() == null || loan.getAnnualIncome() <= 0.0 ||
            loan.getCreditScore() == null || loan.getCreditScore() < 300 || loan.getCreditScore() > 850) {
            throw new IllegalArgumentException("Invalid loan application parameters provided.");
        }

        double requestedAmount = loan.getRequestedAmount();
        double annualIncome = loan.getAnnualIncome();
        double monthlyDebt = loan.getMonthlyDebt() != null ? loan.getMonthlyDebt() : 0.0;
        int creditScore = loan.getCreditScore();

        // [CALCULATION NODE: Monthly Income and Debt-to-Income (DTI) Ratio]
        double monthlyIncome = annualIncome / 12.0;
        double debtToIncomeRatio = monthlyIncome > 0.0 ? (monthlyDebt / monthlyIncome) : 1.0;
        loan.setDebtToIncomeRatio(Math.round(debtToIncomeRatio * 10000.0) / 10000.0);

        // [DECISION NODE: Risk Tier Categorization Based on DTI]
        String riskTier;
        if (debtToIncomeRatio <= 0.36) {
            riskTier = "LOW";
        } else if (debtToIncomeRatio <= 0.45) {
            riskTier = "MEDIUM";
        } else {
            riskTier = "HIGH";
        }
        loan.setRiskTier(riskTier);

        // [CALCULATION NODE: Risk-Adjusted Interest Rate Calculation]
        double baseInterestRate = 0.065; // Base rate 6.50%
        double rateAdjustment = 0.0;

        // [DECISION NODE: Credit Score Rules and Initial Status Check]
        String status;
        if (creditScore < 620) {
            status = "REJECTED";
            rateAdjustment += 0.03;
        } else if (creditScore < 700) {
            rateAdjustment += 0.0125;
            status = "PENDING";
        } else if (creditScore >= 760) {
            rateAdjustment -= 0.005; // Prime discount
            status = "PENDING";
        } else {
            status = "PENDING";
        }

        // [DECISION NODE: Loan-to-Income Exposure Ceiling Check]
        if (requestedAmount > (annualIncome * 4.5)) {
            status = "REFERRED";
            rateAdjustment += 0.01;
        }

        // [DECISION NODE: DTI Upper Threshold Check]
        if (debtToIncomeRatio > 0.50 && !"REJECTED".equals(status)) {
            status = "REJECTED";
        }

        // [CALCULATION NODE: Compute Final Annual Interest Rate]
        double finalInterestRate = baseInterestRate + rateAdjustment;
        if ("HIGH".equals(riskTier)) {
            finalInterestRate += 0.01;
        }
        loan.setInterestRate(Math.round(finalInterestRate * 10000.0) / 10000.0);

        // [CALCULATION NODE: Monthly Amortization Payment (30-year fixed term = 360 months)]
        int totalLoanMonths = 360;
        double monthlyRate = finalInterestRate / 12.0;
        double monthlyPayment;
        if (monthlyRate > 0.0) {
            double rateFactor = Math.pow(1.0 + monthlyRate, totalLoanMonths);
            monthlyPayment = (requestedAmount * monthlyRate * rateFactor) / (rateFactor - 1.0);
        } else {
            monthlyPayment = requestedAmount / totalLoanMonths;
        }
        loan.setMonthlyPayment(Math.round(monthlyPayment * 100.0) / 100.0);

        // [DECISION NODE: Final Approval Status Assignment]
        if (!"REJECTED".equals(status) && !"REFERRED".equals(status)) {
            status = "APPROVED";
        }
        loan.setApprovalStatus(status);

        // [DB CALL: Persist Loan Entity in Repository]
        return loanRepository.save(loan);
    }

    // [FUNCTIONAL FLOW: Batch Processing of Pending Loans]
    @Transactional
    public List<Loan> batchProcessPendingLoans() {
        // [DB CALL: Retrieve All Pending Loans]
        List<Loan> pendingLoans = loanRepository.findByApprovalStatus("PENDING");
        List<Loan> updatedLoans = new ArrayList<>();

        // [LOOP ENTITY: Iterate Through Collection of Loan Entities]
        for (Loan loan : pendingLoans) {
            // [DECISION NODE: Batch Re-assessment Eligibility Check]
            if (loan.getCreditScore() != null && loan.getCreditScore() >= 700 &&
                loan.getDebtToIncomeRatio() != null && loan.getDebtToIncomeRatio() <= 0.40) {

                // [CALCULATION NODE: Promotional Interest Rate Discount Calculation]
                double currentRate = loan.getInterestRate() != null ? loan.getInterestRate() : 0.065;
                double adjustedRate = Math.max(0.045, currentRate - 0.0025);
                loan.setInterestRate(Math.round(adjustedRate * 10000.0) / 10000.0);

                // [CALCULATION NODE: Recalculate Monthly Payment with New Rate]
                double monthlyRate = adjustedRate / 12.0;
                int totalMonths = 360;
                double rateFactor = Math.pow(1.0 + monthlyRate, totalMonths);
                double recalculatedPayment = (loan.getRequestedAmount() * monthlyRate * rateFactor) / (rateFactor - 1.0);
                loan.setMonthlyPayment(Math.round(recalculatedPayment * 100.0) / 100.0);

                // [DECISION NODE: Update Status to Approved]
                loan.setApprovalStatus("APPROVED");
            } else {
                // [DECISION NODE: Fallback Status Decision]
                loan.setApprovalStatus("REJECTED");
            }

            updatedLoans.add(loan);
        }

        // [DB CALL: Batch Update Persisted Entities]
        return loanRepository.saveAll(updatedLoans);
    }

    // [FUNCTIONAL FLOW: Portfolio Summary and Risk Aggregation Analytics]
    @Transactional(readOnly = true)
    public Map<String, Object> calculatePortfolioSummary() {
        // [DB CALL: Retrieve All Loans from Database]
        List<Loan> allLoans = loanRepository.findAll();

        double totalPortfolioExposure = 0.0;
        double weightedRateSum = 0.0;
        double maxLoanAmount = 0.0;
        int approvedCount = 0;
        int rejectedCount = 0;
        int referredCount = 0;
        int pendingCount = 0;

        // [LOOP ENTITY: Iterate Over All Loan Entities for Statistical Calculation]
        for (Loan loan : allLoans) {
            String status = loan.getApprovalStatus();

            // [DECISION NODE: Status Classification and Exposure Tally]
            if ("APPROVED".equalsIgnoreCase(status)) {
                approvedCount++;

                double amount = loan.getRequestedAmount() != null ? loan.getRequestedAmount() : 0.0;
                double rate = loan.getInterestRate() != null ? loan.getInterestRate() : 0.0;

                // [CALCULATION NODE: Aggregate Exposure and Weighted Interest Accumulation]
                totalPortfolioExposure += amount;
                weightedRateSum += (amount * rate);

                // [DECISION NODE: Track Maximum Single Loan Exposure]
                if (amount > maxLoanAmount) {
                    maxLoanAmount = amount;
                }
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                rejectedCount++;
            } else if ("REFERRED".equalsIgnoreCase(status)) {
                referredCount++;
            } else {
                pendingCount++;
            }
        }

        // [CALCULATION NODE: Compute Weighted Average Interest Rate and Approval Rate]
        double weightedAverageRate = totalPortfolioExposure > 0.0 ? (weightedRateSum / totalPortfolioExposure) : 0.0;
        int totalLoans = allLoans.size();
        double approvalRatePercentage = totalLoans > 0 ? ((double) approvedCount / totalLoans) * 100.0 : 0.0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalLoansAnalyzed", totalLoans);
        metrics.put("approvedCount", approvedCount);
        metrics.put("rejectedCount", rejectedCount);
        metrics.put("referredCount", referredCount);
        metrics.put("pendingCount", pendingCount);
        metrics.put("approvalRatePercentage", Math.round(approvalRatePercentage * 100.0) / 100.0);
        metrics.put("totalApprovedExposure", Math.round(totalPortfolioExposure * 100.0) / 100.0);
        metrics.put("maxSingleLoanExposure", Math.round(maxLoanAmount * 100.0) / 100.0);
        metrics.put("weightedAverageInterestRate", Math.round(weightedAverageRate * 10000.0) / 10000.0);

        return metrics;
    }

    // [FUNCTIONAL FLOW: Retrieve All Loans]
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        // [DB CALL: Find All Entities]
        return loanRepository.findAll();
    }

    // [FUNCTIONAL FLOW: Retrieve Loan By ID with Existence Decision]
    @Transactional(readOnly = true)
    public Loan getLoanById(Long id) {
        // [DB CALL: Find Entity by ID]
        Optional<Loan> optionalLoan = loanRepository.findById(id);

        // [DECISION NODE: Check Entity Presence]
        if (optionalLoan.isPresent()) {
            return optionalLoan.get();
        }
        return null;
    }
}
