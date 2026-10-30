package com.hexaware.loanapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByApprovalStatus(String approvalStatus);

    List<Loan> findByRiskTier(String riskTier);
}
