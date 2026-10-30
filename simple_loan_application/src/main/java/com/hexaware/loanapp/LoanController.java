package com.hexaware.loanapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // [ENDPOINT FLOW: Submit & Evaluate Loan Application]
    @PostMapping("/apply")
    public ResponseEntity<Loan> applyLoan(@RequestBody Loan loan) {
        Loan evaluatedLoan = loanService.evaluateAndProcessLoan(loan);
        return new ResponseEntity<>(evaluatedLoan, HttpStatus.CREATED);
    }

    // [ENDPOINT FLOW: Trigger Batch Re-evaluation of Pending Loans]
    @PostMapping("/batch-process")
    public ResponseEntity<List<Loan>> batchProcessLoans() {
        List<Loan> processedLoans = loanService.batchProcessPendingLoans();
        return ResponseEntity.ok(processedLoans);
    }

    // [ENDPOINT FLOW: Portfolio Statistical and Risk Metrics Summary]
    @GetMapping("/portfolio-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary() {
        Map<String, Object> summary = loanService.calculatePortfolioSummary();
        return ResponseEntity.ok(summary);
    }

    // [ENDPOINT FLOW: Retrieve All Loan Records]
    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    // [ENDPOINT FLOW: Retrieve Single Loan Record by ID]
    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        if (loan != null) {
            return ResponseEntity.ok(loan);
        }
        return ResponseEntity.notFound().build();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(LoanController.class);
        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("spring.datasource.url", "jdbc:postgresql://localhost:5432/loandb");
        defaultProperties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        defaultProperties.put("spring.datasource.username", "postgres");
        defaultProperties.put("spring.datasource.password", "postgres");
        defaultProperties.put("spring.jpa.hibernate.ddl-auto", "update");
        defaultProperties.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        app.setDefaultProperties(defaultProperties);
        app.run(args);
    }
}
