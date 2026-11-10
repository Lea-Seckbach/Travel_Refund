package com.qa.travelrefund;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Post-release sanity: travel refund for an employee who did NOT work all the
 * work days of the month.
 *
 * Follows the "sanity order" in the test plan:
 *  1. Travel data for the month is 0 before calculation
 *  2. Module is available after deployment
 *  3. Calculation can be triggered without errors
 *  4. Amount = independent reference calculation (actual days, not all days)
 *  5. Amount appears on the payslip
 *  6. Report amount == payslip amount
 *  7. Recalculation for an unchanged employee does not change the amount
 */
@DisplayName("Sanity - travel refund for employee who did not work all days")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SanityPartialWorkDaysTest {

    private static final Employee EMP = TestConfig.PARTIAL_DAYS_EMPLOYEE;
    private static final String MONTH = TestConfig.TEST_MONTH;

    private TravelRefundApi api;
    private BigDecimal calculatedAmount;

    @BeforeAll
    void setUp() {
        api = TestConfig.BASE_URL.isEmpty()
                ? new SimulatedTravelRefundApi(EMP)
                : new HttpTravelRefundApi(TestConfig.BASE_URL);

        // Guard: test data must really be a partial month, or this test proves nothing.
        assertTrue(EMP.actualWorkDays > 0 && EMP.actualWorkDays < EMP.monthWorkDays,
                "Test data error: actual work days (" + EMP.actualWorkDays
                        + ") must be between 1 and month work days - 1 (" + EMP.monthWorkDays + ")");
    }

    @Test
    @Order(1)
    @DisplayName("1. Travel refund for the month is 0 before calculation")
    void refundIsZeroBeforeCalculation() {
        BigDecimal before = api.getStoredRefund(EMP.id, MONTH);
        assertEquals(0, before.signum(),
                "Travel refund should be 0 before calculation (previous month not reset?), got " + before);
    }

    @Test
    @Order(2)
    @DisplayName("2. Module is available after deployment")
    void moduleIsAvailable() {
        assertTrue(api.isHealthy(), "Travel refund module is not available");
    }

    @Test
    @Order(3)
    @DisplayName("3. Calculation runs and returns an amount without error")
    void calculationRunsWithoutError() {
        TravelRefundApi.CalculationResult result = api.calculate(EMP.id, MONTH);

        assertTrue(result.success, "Calculation failed: " + result.error);
        assertNotNull(result.amount, "Calculation returned no amount");
        assertTrue(result.amount.signum() >= 0, "Refund is negative: " + result.amount);
        calculatedAmount = result.amount;
    }

    @Test
    @Order(4)
    @DisplayName("4. Amount matches reference calculation based on ACTUAL work days")
    void amountMatchesActualDays() {
        requireCalculated();
        BigDecimal expected = ReferenceCalculator.expectedRefund(EMP);
        BigDecimal fullMonth = ReferenceCalculator.expectedRefund(EMP.withFullMonth());

        // Complaint #1/#2 in the doc: refund computed on all days instead of actual days.
        assertTrue(calculatedAmount.compareTo(fullMonth) != 0 || expected.compareTo(fullMonth) == 0,
                "Employee worked " + EMP.actualWorkDays + "/" + EMP.monthWorkDays
                        + " days but got the full-month refund " + fullMonth);

        assertEquals(0, expected.compareTo(calculatedAmount),
                "Expected " + expected + " (rate " + ReferenceCalculator.dailyRate(EMP.distanceKm)
                        + " x " + EMP.jobPercent + "% x " + EMP.actualWorkDays + " days), got " + calculatedAmount);
    }

    @Test
    @Order(5)
    @DisplayName("5. Amount appears on the payslip")
    void amountOnPayslip() {
        requireCalculated();
        BigDecimal payslip = api.getPayslipTravelAmount(EMP.id, MONTH);
        assertEquals(0, calculatedAmount.compareTo(payslip),
                "Payslip shows " + payslip + ", calculation returned " + calculatedAmount);
    }

    @Test
    @Order(6)
    @DisplayName("6. Report amount equals payslip amount")
    void reportEqualsPayslip() {
        requireCalculated();
        BigDecimal payslip = api.getPayslipTravelAmount(EMP.id, MONTH);
        BigDecimal report = api.getReportTravelAmount(EMP.id, MONTH);
        assertEquals(0, payslip.compareTo(report),
                "Report (" + report + ") differs from payslip (" + payslip + "); payslip is binding");
    }

    @Test
    @Order(7)
    @DisplayName("7. Recalculation for unchanged employee gives the same amount")
    void recalculationIsStable() {
        requireCalculated();
        TravelRefundApi.CalculationResult again = api.calculate(EMP.id, MONTH);

        assertTrue(again.success, "Recalculation failed: " + again.error);
        assertEquals(0, calculatedAmount.compareTo(again.amount),
                "Recalculation changed the amount from " + calculatedAmount + " to " + again.amount);
        assertEquals(0, calculatedAmount.compareTo(api.getPayslipTravelAmount(EMP.id, MONTH)),
                "Payslip changed after recalculation");
    }

    private void requireCalculated() {
        assumeTrue(calculatedAmount != null, "Skipped: calculation step (3) did not succeed");
    }
}
