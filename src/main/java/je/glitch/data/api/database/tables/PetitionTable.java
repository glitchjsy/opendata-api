package je.glitch.data.api.database.tables;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.http.Context;
import je.glitch.data.api.utils.ErrorType;
import je.glitch.data.api.utils.HttpException;
import je.glitch.data.api.utils.Utils;
import lombok.RequiredArgsConstructor;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PetitionTable implements ITable {
    private final HikariDataSource dataSource;

    public Map<String, Object> getPetitions(Context ctx) {
        try (Connection connection = dataSource.getConnection()) {

            Utils.QueryDateResult dateResult = Utils.queryDateSql(
                    "createdAt",
                    ctx.queryParam("startDate"),
                    ctx.queryParam("endDate")
            );

            boolean includeFull = ctx.queryParam("includeFull") != null;

            StringBuilder where = new StringBuilder("FROM petitions p");
            List<Object> params = new ArrayList<>(dateResult.getDateParams());

            if (!dateResult.getDateSql().isEmpty()) {
                where.append(" ").append(dateResult.getDateSql());
            }

            addFilter(where, params, "p.title LIKE ?", ctx.queryParam("title"));
            addFilter(where, params, "p.summary LIKE ?", ctx.queryParam("summary"));
            addFilter(where, params, "p.description LIKE ?", ctx.queryParam("description"));
            addFilter(where, params, "p.state = ?", ctx.queryParam("state"));
            addFilter(where, params, "p.creatorName LIKE ?", ctx.queryParam("creator"));

            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(30);
            int offset = (page - 1) * limit;

            int totalItems = fetchSingleInt(connection, "SELECT COUNT(*) " + where, params);
            int totalPages = (int) Math.ceil((double) totalItems / limit);

            List<Map<String, Object>> baseRows = fetchRows(connection,
                    "SELECT * " + where + " ORDER BY p.id DESC LIMIT ? OFFSET ?",
                    params, limit, offset
            );

            List<Object> results = new ArrayList<>();

            if (!includeFull) {
                results.addAll(baseRows);
            } else {
                if (!baseRows.isEmpty()) {
                    List<Long> petitionIds = baseRows.stream()
                            .map(r -> ((Number) r.get("id")).longValue())
                            .toList();

                    Map<Long, Map<String, Object>> responsesByPetition =
                            fetchSingleRowMap(connection,
                                    "SELECT * FROM petitionMinistersResponses WHERE petitionId IN (" +
                                            placeholders(petitionIds.size()) + ")",
                                    petitionIds
                            );

                    Map<Long, Map<String, Object>> debatesByPetition =
                            fetchSingleRowMap(connection,
                                    "SELECT * FROM petitionDebates WHERE petitionId IN (" +
                                            placeholders(petitionIds.size()) + ")",
                                    petitionIds
                            );

                    Map<Long, List<Map<String, Object>>> signaturesByPetition =
                            fetchSignaturesByPetitionIds(connection, petitionIds);

                    for (Map<String, Object> base : baseRows) {
                        Long id = ((Number) base.get("id")).longValue();
                        Map<String, Object> full = new HashMap<>(base);
                        full.put("response", responsesByPetition.get(id));
                        full.put("debate", debatesByPetition.get(id));
                        full.put("signaturesByParish", signaturesByPetition.getOrDefault(id, List.of()));
                        results.add(full);
                    }
                }
            }

            return Map.of(
                    "pagination", Map.of(
                            "page", page,
                            "limit", limit,
                            "totalPages", totalPages,
                            "totalItems", totalItems
                    ),
                    "results", results
            );
        } catch (Exception ex) {
            throw new HttpException(ErrorType.SERVER_ERROR, 500, ex.getMessage());
        }
    }

    public Map<String, Object> getPetitionStats() throws SQLException {
        // Top 10 petitions by signature count
        String sqlTopPetitions = """
        SELECT id, title, signatureCount
        FROM petitions
        ORDER BY petitions.signatureCount DESC
        LIMIT 10
    """;

        // Total signatures per parish
        String sqlSignaturesByParish = """
        SELECT parishName, SUM(ps.signatureCount) AS totalSignatures
        FROM petitionSignaturesByParish ps
        JOIN petitions p ON ps.petitionId = p.id
        GROUP BY parishName
    """;

        // Total petitions by state
        String sqlPetitionsByState = """
        SELECT state, COUNT(*) AS total
        FROM petitions
        GROUP BY state
    """;

        // Petitions debated by the states assembly
        String sqlDebatedPetitions = """
        SELECT COUNT(DISTINCT petitionId) AS total
        FROM petitionDebates
        WHERE debatedOn IS NOT NULL
    """;

        // Petitions with minister response
        String sqlPetitionsWithResponses = """
        SELECT COUNT(DISTINCT petitionId) AS total
        FROM petitionMinistersResponses
    """;

        // Petitions created per year
        String sqlPetitionsPerYear = """
        SELECT YEAR(createdAt) AS year, COUNT(*) AS total
        FROM petitions
        GROUP BY YEAR(createdAt)
        ORDER BY YEAR(createdAt)
    """;

        // Responses per year
        String sqlResponsesPerYear = """
        SELECT YEAR(publishedOn) AS year, COUNT(*) AS total
        FROM petitionMinistersResponses
        WHERE publishedOn IS NOT NULL
        GROUP BY YEAR(publishedOn)
        ORDER BY YEAR(publishedOn)
    """;

        // Debates per year
        String sqlDebatesPerYear = """
        SELECT YEAR(debatedOn) AS year, COUNT(*) AS total
        FROM petitionDebates
        WHERE debatedOn IS NOT NULL
        GROUP BY YEAR(debatedOn)
        ORDER BY YEAR(debatedOn)
    """;

        Map<String, Object> results = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // Top petitions
            try (PreparedStatement stmt = conn.prepareStatement(sqlTopPetitions)) {
                List<Map<String, Object>> topPetitions = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> petition = new HashMap<>();
                        petition.put("id", rs.getLong("id"));
                        petition.put("title", rs.getString("title"));
                        petition.put("signatureCount", rs.getInt("signatureCount"));
                        topPetitions.add(petition);
                    }
                }
                results.put("topPetitions", topPetitions);
            }

            // Signatures per parish
            try (PreparedStatement stmt = conn.prepareStatement(sqlSignaturesByParish)) {
                Map<String, Integer> signaturesByParish = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        signaturesByParish.put(rs.getString("parishName"), rs.getInt("totalSignatures"));
                    }
                }
                results.put("signaturesByParish", signaturesByParish);
            }

            // Petitions by state
            try (PreparedStatement stmt = conn.prepareStatement(sqlPetitionsByState)) {
                Map<String, Integer> petitionsByState = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        petitionsByState.put(rs.getString("state"), rs.getInt("total"));
                    }
                }
                results.put("petitionsByState", petitionsByState);
            }

            // Debated petitions
            try (PreparedStatement stmt = conn.prepareStatement(sqlDebatedPetitions)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) results.put("debatedPetitions", rs.getInt("total"));
                }
            }

            // Petitions with minister response
            try (PreparedStatement stmt = conn.prepareStatement(sqlPetitionsWithResponses)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) results.put("petitionsWithResponses", rs.getInt("total"));
                }
            }

            // Petitions per year
            try (PreparedStatement stmt = conn.prepareStatement(sqlPetitionsPerYear)) {
                Map<Integer, Integer> petitionsPerYear = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) petitionsPerYear.put(rs.getInt("year"), rs.getInt("total"));
                }
                results.put("petitionsPerYear", petitionsPerYear);
            }

            // Responses per year
            try (PreparedStatement stmt = conn.prepareStatement(sqlResponsesPerYear)) {
                Map<Integer, Integer> responsesPerYear = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) responsesPerYear.put(rs.getInt("year"), rs.getInt("total"));
                }
                results.put("responsesPerYear", responsesPerYear);
            }

            // Debates per year
            try (PreparedStatement stmt = conn.prepareStatement(sqlDebatesPerYear)) {
                Map<Integer, Integer> debatesPerYear = new HashMap<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) debatesPerYear.put(rs.getInt("year"), rs.getInt("total"));
                }
                results.put("debatesPerYear", debatesPerYear);
            }
        }

        return results;
    }

    private String placeholders(int count) {
        return count > 0 ? String.join(",", Collections.nCopies(count, "?")) : "NULL";
    }

    private Map<Long, Map<String, Object>> fetchSingleRowMap(Connection conn, String sql, List<Long> ids) throws SQLException {
        List<Map<String, Object>> rows = fetchRowsNoPagination(conn, sql, new ArrayList<>(ids));
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Number n = (Number) row.get("petitionId");
            if (n != null) {
                map.put(n.longValue(), row);
            }
        }
        return map;
    }

    private Map<Long, List<Map<String, Object>>> fetchSignaturesByPetitionIds(Connection conn, List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return Map.of();

        String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM petitionSignaturesByParish WHERE petitionId IN (" + placeholders + ") ORDER BY parishName ASC";

        List<Object> params = new ArrayList<>(ids);
        List<Map<String, Object>> rows = fetchRowsNoPagination(conn, sql, params);

        return groupByLongKey(rows, "petitionId");
    }

    private Map<Long, List<Map<String, Object>>> groupByLongKey(List<Map<String, Object>> rows, String key) {
        Map<Long, List<Map<String, Object>>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Number n = (Number) row.get(key);
            if (n == null) continue;
            long k = n.longValue();
            map.computeIfAbsent(k, x -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static void addFilter(StringBuilder query, List<Object> params, String condition, String value) {
        if (value != null) {
            query.append(query.toString().contains("WHERE") ? " AND " : " WHERE ").append(condition);

            String newValue = value;
            if (condition.contains("LIKE")) {
                newValue = "%" + value + "%";
            }
            params.add(newValue);
        }
    }

    private static int fetchSingleInt(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static List<Map<String, Object>> fetchRowsNoPagination(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value;
                    int type = meta.getColumnType(i);
                    if (type == Types.TIMESTAMP || type == Types.DATE) {
                        Timestamp ts = rs.getTimestamp(i);
                        value = ts != null ? ts.toLocalDateTime().toString() : null;
                    } else {
                        value = rs.getObject(i);
                    }
                    row.put(meta.getColumnName(i), value);
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private static List<Map<String, Object>> fetchRows(Connection conn, String sql, List<Object> params, int limit, int offset) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParams(stmt, params);
            stmt.setInt(params.size() + 1, limit);
            stmt.setInt(params.size() + 2, offset);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value;
                    int type = meta.getColumnType(i);
                    if (type == Types.TIMESTAMP || type == Types.DATE) {
                        Timestamp ts = rs.getTimestamp(i);
                        value = ts != null ? ts.toLocalDateTime().toString() : null;
                    } else {
                        value = rs.getObject(i);
                    }
                    row.put(meta.getColumnName(i), value);
                }
                rows.add(row);
            }

            return rows;
        }
    }

    private static void setParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }
}
