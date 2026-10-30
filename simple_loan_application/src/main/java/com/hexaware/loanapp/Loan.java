package com.hexaware.loanapp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicantName;
    private Double requestedAmount;
    private Integer creditScore;
    private Double annualIncome;
    private Double monthlyDebt;
    private Double debtToIncomeRatio;
    private Double interestRate;
    private Double monthlyPayment;
    private String riskTier;
    private String approvalStatus;

    public Loan() {
    }

    public Loan(String applicantName, Double requestedAmount, Integer creditScore, Double annualIncome, Double monthlyDebt) {
        this.applicantName = applicantName;
        this.requestedAmount = requestedAmount;
        this.creditScore = creditScore;
        this.annualIncome = annualIncome;
        this.monthlyDebt = monthlyDebt;
        this.approvalStatus = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Double getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(Double requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public Double getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(Double annualIncome) {
        this.annualIncome = annualIncome;
    }

    public Double getMonthlyDebt() {
        return monthlyDebt;
    }

    public void setMonthlyDebt(Double monthlyDebt) {
        this.monthlyDebt = monthlyDebt;
    }

    public Double getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(Double debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public Double getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(Double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(String riskTier) {
        this.riskTier = riskTier;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
}
