package je.glitch.data.api.utils;

import com.google.gson.JsonObject;
import io.javalin.http.Context;
import je.glitch.data.api.Config;
import je.glitch.data.api.Server;
import je.glitch.data.api.database.MySQLConnection;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class Utils {
    public static final Pattern DATE_FORMAT = Pattern.compile("^\\d{4}[-/]\\d{2}[-/]\\d{2}$");

    private static final RuntimeMXBean RUNTIME_BEAN = ManagementFactory.getRuntimeMXBean();

    public static QueryDateResult queryDateSql(String dateField, String startDate, String endDate) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            if (!DATE_FORMAT.matcher(startDate).matches()) {
                throw new IllegalArgumentException("Invalid start date. Please use the format YYYY/MM/DD.");
            }
            conditions.add(dateField + " >= ?");
            params.add(startDate);
        }

        if (endDate != null) {
            if (!DATE_FORMAT.matcher(endDate).matches()) {
                throw new IllegalArgumentException("Invalid end date. Please use the format YYYY/MM/DD.");
            }
            conditions.add(dateField + " <= ?");
            params.add(endDate);
        }

        String dateSql = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
        return new QueryDateResult(dateSql, params);
    }

    public static HttpResponse<String> sendRequest(HttpClient client, String url, String method, String body) throws IOException, InterruptedException {
        return sendRequest(client, url, method, body, null);
    }

    public static HttpResponse<String> sendRequest(HttpClient client, String url, String method, String body, String cookies) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7;application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        if (cookies != null) {
            requestBuilder.header("Cookie", cookies);
        }

        if (method.equalsIgnoreCase("POST") && body != null) {
            requestBuilder.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            requestBuilder.GET();
        }

        log.info("Outgoing request: " + method + " " + url);

        return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static String getUptimeString() {
        long uptimeMillis = RUNTIME_BEAN.getUptime();
        long seconds = uptimeMillis / 1000;

        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd, %dh, %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh, %dm, %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm, %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    public static String parseDateToISO(String input) {
        if (input == null || input.isEmpty()) return null;

        // Formatter for full month names, e.g., "February"
        DateTimeFormatter fullMonthFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("d ")
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(Locale.ENGLISH);

        // Formatter for abbreviated month names, e.g., "Feb"
        DateTimeFormatter shortMonthFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("d ")
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                .appendLiteral(' ')
                .appendValue(ChronoField.YEAR, 4)
                .toFormatter(Locale.ENGLISH);

        try {
            LocalDate date = LocalDate.parse(input, fullMonthFormatter);
            return date.toString(); // "YYYY-MM-DD"
        } catch (DateTimeParseException e) {
            try {
                LocalDate date = LocalDate.parse(input, shortMonthFormatter);
                return date.toString();
            } catch (DateTimeParseException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static boolean isAuthenticatedPublic(Context ctx, MySQLConnection connection) {
        String tokenHeader = ctx.header("x-api-key");
        String tokenQuery = ctx.queryParam("auth");

        String apiTokenId = null;
        String token = null;

        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            token = tokenHeader;
        } else if (tokenQuery != null && !tokenQuery.isEmpty()) {
            token = tokenQuery;
        }
        if (token != null) {
            String foundId = connection.getApiKeyTable().getIdFromKey(token);
            apiTokenId = (foundId != null && !foundId.isEmpty()) ? foundId : null;
        }
        return apiTokenId != null;
    }

    public static <T> java.lang.reflect.Type listOf(Class<T> clazz) {
        return com.google.gson.reflect.TypeToken.getParameterized(List.class, clazz).getType();
    }

    public static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Integer optionalInt(String raw) {
        if (raw == null) {
            return null;
        }
        return parseInt(raw);
    }

    public static Double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Double optionalDouble(String raw) {
        if (raw == null) {
            return null;
        }
        return parseDouble(raw);
    }

    public static boolean verifyRecaptchaToken(String token) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = "https://www.google.com/recaptcha/api/siteverify?secret=" + Config.getRecaptchaSecret() + "&response=" + token;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = Server.GSON.fromJson(response.body(), JsonObject.class);

            return json.has("success") && json.get("success").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Data
    public static class QueryDateResult {
        private final String dateSql;
        private final List<Object> dateParams;
    }
}
