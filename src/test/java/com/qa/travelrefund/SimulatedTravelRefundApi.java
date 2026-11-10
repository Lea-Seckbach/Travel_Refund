package com.qa.travelrefund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory stand-in for the system, so the test can run without a real
 * environment. It implements the rules ITS OWN way (not by calling
 * ReferenceCalculator), so the test still compares two separate calculations.
 */
public class SimulatedTravelRefundApi implements TravelRefundApi {

    private final Map<String, Employee> employees = new HashMap<>();
    private final Map<String, BigDecimal> stored = new HashMap<>();   // payslip = binding source

    public SimulatedTravelRefundApi(Employee... seed) {
        for (Employee e : seed) employees.put(e.id, e);
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public BigDecimal getStoredRefund(String employeeId, String month) {
        return stored.getOrDefault(key(employeeId, month), BigDecimal.ZERO);
    }

    @Override
    public CalculationResult calculate(String employeeId, String month) {
        Employee e = employees.get(employeeId);
        if (e == null) {
            return new CalculationResult(false, null, "Employee not found: " + employeeId);
        }
        BigDecimal amount;
        if (e.type == Employee.Type.SHUTTLE) {
            amount = BigDecimal.ZERO;
        } else {
            double rate = e.distanceKm <= 20 ? 11.00 : e.distanceKm <= 50 ? 22.60 : 30.00;
            amount = new BigDecimal(String.valueOf(rate))
                    .multiply(new BigDecimal(e.actualWorkDays))
                    .multiply(new BigDecimal(e.jobPercent))
                    .divide(new BigDecimal(100))
                    .min(new BigDecimal("600.00"));
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        stored.put(key(employeeId, month), amount);
        return new CalculationResult(true, amount, null);
    }

    @Override
    public BigDecimal getPayslipTravelAmount(String employeeId, String month) {
        return getStoredRefund(employeeId, month);
    }

    @Override
    public BigDecimal getReportTravelAmount(String employeeId, String month) {
        return getStoredRefund(employeeId, month);
    }

    private static String key(String id, String month) {
        return id + "|" + month;
    }
}
