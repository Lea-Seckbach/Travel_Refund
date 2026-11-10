package com.qa.travelrefund;

import java.math.BigDecimal;

/**
 * Test configuration and business-rule ASSUMPTIONS.
 * The requirements document leaves these open ("questions for product/dev") -
 * update them once product confirms the real values.
 */
public final class TestConfig {

    private TestConfig() { }

    // ---- Where to run --------------------------------------------------------
    // Empty -> built-in simulator (no real system needed).
    // Set   -> real test environment, e.g. -Dtravel.api.url=https://test-env/api
    public static final String BASE_URL = prop("travel.api.url", "");
    public static final String API_TOKEN = prop("travel.api.token", "");
    public static final int TIMEOUT_MS = 30_000;

    /** Month under test (YYYY-MM). */
    public static final String TEST_MONTH = prop("test.month", "2026-10");

    // ---- Test employee: company employee, full-time, did NOT work all days ---
    public static final Employee PARTIAL_DAYS_EMPLOYEE = new Employee(
            prop("partial.employee.id", "EMP-1001"),
            Employee.Type.COMPANY,
            "Modiin",          // residence
            "Tel Aviv",        // workplace
            35.0,              // distance in km -> 20-50 km band
            100,               // job percent
            22,                // work days in the month
            15                 // days actually worked (< 22)
    );

    // ---- Business rules (ASSUMED - confirm with product) ---------------------
    /** Upper bound (inclusive) in km of each distance band. */
    public static final double[] BAND_UPPER_KM = {20.0, 50.0, Double.MAX_VALUE};
    /** Daily refund rate in NIS for each band above. */
    public static final BigDecimal[] BAND_DAILY_RATE = {
            new BigDecimal("11.00"), new BigDecimal("22.60"), new BigDecimal("30.00")
    };
    public static final BigDecimal MONTHLY_CAP = new BigDecimal("600.00");
    public static final int ROUND_SCALE = 2;   // agorot

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        return (v == null || v.isEmpty()) ? def : v;
    }
}
