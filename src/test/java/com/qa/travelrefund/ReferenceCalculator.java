package com.qa.travelrefund;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Independent reference calculation ("run the calculation separately to verify
 * the expected result" - sanity plan). Never calls the system under test.
 *
 * refund = dailyRate(distance band) x jobPercent x ACTUAL work days, capped, rounded.
 */
public final class ReferenceCalculator {

    private ReferenceCalculator() { }

    public static BigDecimal dailyRate(double distanceKm) {
        for (int i = 0; i < TestConfig.BAND_UPPER_KM.length; i++) {
            if (distanceKm <= TestConfig.BAND_UPPER_KM[i]) {
                return TestConfig.BAND_DAILY_RATE[i];
            }
        }
        throw new IllegalArgumentException("No band for distance " + distanceKm);
    }

    public static BigDecimal expectedRefund(Employee e) {
        if (e.type == Employee.Type.SHUTTLE) {
            return BigDecimal.ZERO.setScale(TestConfig.ROUND_SCALE);   // not eligible
        }
        BigDecimal amount = dailyRate(e.distanceKm)
                .multiply(BigDecimal.valueOf(e.jobPercent))
                .divide(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(e.actualWorkDays));

        if (amount.compareTo(TestConfig.MONTHLY_CAP) > 0) {
            amount = TestConfig.MONTHLY_CAP;
        }
        return amount.setScale(TestConfig.ROUND_SCALE, RoundingMode.HALF_UP);
    }
}
