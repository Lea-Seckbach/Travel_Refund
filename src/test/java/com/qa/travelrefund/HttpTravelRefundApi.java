package com.qa.travelrefund;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client for the real test environment.
 * ENDPOINTS ARE ASSUMED - replace the paths/field names with the real ones.
 */
public class HttpTravelRefundApi implements TravelRefundApi {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl;

    public HttpTravelRefundApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public boolean isHealthy() {
        try {
            return send("GET", "/health", null).status == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public BigDecimal getStoredRefund(String employeeId, String month) {
        return amountFrom(get("/travel-refunds?employeeId=" + enc(employeeId) + "&month=" + enc(month)));
    }

    @Override
    public CalculationResult calculate(String employeeId, String month) {
        try {
            String body = JSON.createObjectNode()
                    .put("employeeId", employeeId)
                    .put("month", month)
                    .toString();
            Response r = send("POST", "/travel-refunds/calculate", body);
            if (r.status != 200) {
                return new CalculationResult(false, null, "HTTP " + r.status + ": " + r.body);
            }
            JsonNode n = JSON.readTree(r.body);
            return new CalculationResult(true, n.get("amount").decimalValue(), null);
        } catch (IOException e) {
            return new CalculationResult(false, null, e.toString());
        }
    }

    @Override
    public BigDecimal getPayslipTravelAmount(String employeeId, String month) {
        return amountFrom(get("/payslips/" + enc(employeeId) + "/" + enc(month) + "/travel"));
    }

    @Override
    public BigDecimal getReportTravelAmount(String employeeId, String month) {
        return amountFrom(get("/reports/travel?employeeId=" + enc(employeeId) + "&month=" + enc(month)));
    }

    // ---- helpers --------------------------------------------------------------

    private JsonNode get(String path) {
        try {
            Response r = send("GET", path, null);
            if (r.status != 200) {
                throw new AssertionError("GET " + path + " returned HTTP " + r.status + ": " + r.body);
            }
            return JSON.readTree(r.body);
        } catch (IOException e) {
            throw new AssertionError("GET " + path + " failed: " + e, e);
        }
    }

    private static BigDecimal amountFrom(JsonNode n) {
        JsonNode amount = n.get("amount");
        if (amount == null || amount.isNull()) {
            throw new AssertionError("Response has no 'amount' field: " + n);
        }
        // Complaint #3 in the doc: report vs payslip type mismatch (string vs number).
        if (!amount.isNumber()) {
            throw new AssertionError("'amount' is not numeric (got " + amount.getNodeType() + "): " + n);
        }
        return amount.decimalValue();
    }

    private Response send(String method, String path, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(TestConfig.TIMEOUT_MS);
        c.setReadTimeout(TestConfig.TIMEOUT_MS);
        c.setRequestProperty("Accept", "application/json");
        if (!TestConfig.API_TOKEN.isEmpty()) {
            c.setRequestProperty("Authorization", "Bearer " + TestConfig.API_TOKEN);
        }
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = c.getResponseCode();
        InputStream is = status >= 400 ? c.getErrorStream() : c.getInputStream();
        return new Response(status, read(is));
    }

    private static String read(InputStream is) throws IOException {
        if (is == null) return "";
        try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Response {
        final int status;
        final String body;
        Response(int status, String body) { this.status = status; this.body = body; }
    }
}
