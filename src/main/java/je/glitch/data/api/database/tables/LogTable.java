package je.glitch.data.api.database.tables;

import com.zaxxer.hikari.HikariDataSource;
import je.glitch.data.api.modelsnew.entities.ApiRequestStatsEntity;
import je.glitch.data.api.modelsnew.entities.DailyRequestStatEntity;
import je.glitch.data.api.modelsnew.entities.EndpointRequestStatEntity;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class LogTable implements ITable {
    private final HikariDataSource dataSource;

    public boolean trackRequest(String method, String path, int statusCode, String ipAddress, String userAgent, String apiTokenId) {
        String sql = """
                INSERT INTO apiRequests (id, method, path, statusCode, ipAddress, userAgent, apiTokenId)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        String id = UUID.randomUUID().toString();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.setString(2, method);
            stmt.setString(3, path);
            stmt.setInt(4, statusCode);
            stmt.setString(5, ipAddress);
            stmt.setString(6, userAgent);

            if (apiTokenId != null) {
                stmt.setString(7, apiTokenId);
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
            }
            return stmt.executeUpdate() > 0;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    public ApiRequestStatsEntity getRequestStats() {
        String sql = """
                    SELECT
                        (SELECT COUNT(*) FROM apiRequests) AS totalAllTime,
                        (SELECT COUNT(*) FROM apiRequests WHERE createdAt >= NOW() - INTERVAL 1 DAY) AS total24Hours,
                        (SELECT COUNT(*) FROM apiRequests WHERE createdAt >= NOW() - INTERVAL 7 DAY) AS total7Days,
                        (SELECT COUNT(*) FROM apiRequests WHERE createdAt >= NOW() - INTERVAL 30 DAY) AS total30Days
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return new ApiRequestStatsEntity(
                        rs.getLong("totalAllTime"),
                        rs.getLong("total24Hours"),
                        rs.getLong("total7Days"),
                        rs.getLong("total30Days")
                );
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return new ApiRequestStatsEntity(0, 0, 0, 0);
    }

    public List<DailyRequestStatEntity> getDailyStatsForMonth(int year, int month) {
        String sql = """
                    SELECT DATE(createdAt) AS day,
                           CASE WHEN apiTokenId IS NULL THEN 'unauthenticated' ELSE 'authenticated' END AS authStatus,
                           COUNT(*) AS total
                    FROM apiRequests
                    WHERE YEAR(createdAt) = ? AND MONTH(createdAt) = ?
                    GROUP BY DATE(createdAt), authStatus
                    ORDER BY day ASC
                """;

        List<DailyRequestStatEntity> stats = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    stats.add(new DailyRequestStatEntity(
                            rs.getString("day"),
                            rs.getString("authStatus"),
                            rs.getLong("total")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return stats;
    }

    public List<EndpointRequestStatEntity> getTopEndpoints(Integer year, Integer month) {
        String baseSql = """
                    SELECT path, COUNT(*) AS total
                    FROM apiRequests
                """;

        String whereClause = "";
        if (year != null && month != null && year > 0 && month > 0) {
            whereClause = "WHERE YEAR(createdAt) = ? AND MONTH(createdAt) = ?";
        }

        String groupOrderLimit = " GROUP BY path ORDER BY total DESC LIMIT 20";
        String sql = baseSql + " " + whereClause + groupOrderLimit;

        List<EndpointRequestStatEntity> stats = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            if (!whereClause.isEmpty()) {
                stmt.setInt(1, year);
                stmt.setInt(2, month);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    stats.add(new EndpointRequestStatEntity(
                            rs.getString("path"),
                            rs.getLong("total")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error fetching top endpoints: " + ex.getMessage());
        }

        return stats;
    }
}
