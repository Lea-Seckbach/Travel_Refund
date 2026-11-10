package com.qa.travelrefund;

import java.math.BigDecimal;

/** What the sanity test needs from the system under test. */
public interface TravelRefundApi {

    /** Module is up after deployment. */
    boolean isHealthy();

    /** Stored travel refund for the employee/month (0 before calculation). */
    BigDecimal getStoredRefund(String employeeId, String month);

    /** Trigger the calculation manually for one employee. */
    CalculationResult calculate(String employeeId, String month);

    /** Travel amount on the employee's payslip (the binding source). */
    BigDecimal getPayslipTravelAmount(String employeeId, String month);

    /** Travel amount in the system report (display only). */
    BigDecimal getReportTravelAmount(String employeeId, String month);

    final class CalculationResult {
        public final boolean success;
        public final BigDecimal amount;
        public final String error;

        public CalculationResult(boolean success, BigDecimal amount, String error) {
            this.success = success;
            this.amount = amount;
            this.error = error;
        }
    }
}
