package je.glitch.data.api.database.tables;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import je.glitch.data.api.models.Carpark;
import je.glitch.data.api.models.LiveParkingSpace;
import je.glitch.data.api.models.carpark.BusiestCarpark;
import je.glitch.data.api.models.carpark.CarparkAvailability;
import je.glitch.data.api.models.carpark.CarparkFullDay;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CarparkTable implements ITable {
    private final HikariDataSource dataSource;

    /**
     * Returns all car parks in the database.
     * @return a list of car parks
     */
    public List<Carpark> getCarparks() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("""
                            SELECT 
                                carparks.*, 
                                companies.name AS ownerName, 
                                GROUP_CONCAT(carparkPaymentMethods.paymentMethod) AS paymentMethods 
                            FROM carparks 
                            LEFT JOIN companies ON companies.id = carparks.ownerId 
                            LEFT JOIN carparkPaymentMethods ON carparkPaymentMethods.carparkId = carparks.id 
                            GROUP BY carparks.id
                    """);

            try (ResultSet result = stmt.executeQuery()) {
                List<Carpark> carparks = new ArrayList<>();

                while (result.next()) {
                    carparks.add(Carpark.of(result));
                }
                return carparks;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<>();
        }
    }

    public Carpark getCarparkById(String id) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("""
                            SELECT 
                                carparks.*, 
                                companies.name AS ownerName, 
                                GROUP_CONCAT(carparkPaymentMethods.paymentMethod) AS paymentMethods 
                            FROM carparks 
                            LEFT JOIN companies ON companies.id = carparks.ownerId 
                            LEFT JOIN carparkPaymentMethods ON carparkPaymentMethods.carparkId = carparks.id 
                            WHERE carparks.id = ?
                    """);

            stmt.setString(1, id);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return Carpark.of(result);
                }
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public Carpark getCarparkByLiveTrackingCode(String code) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("""
                            SELECT 
                                carparks.*, 
                                companies.name AS ownerName, 
                                GROUP_CONCAT(carparkPaymentMethods.paymentMethod) AS paymentMethods 
                            FROM carparks 
                            LEFT JOIN companies ON companies.id = carparks.ownerId 
                            LEFT JOIN carparkPaymentMethods ON carparkPaymentMethods.carparkId = carparks.id 
                            WHERE carparks.liveTrackingCode = ?
                            GROUP BY carparks.id
                    """);

            stmt.setString(1, code);

            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    return Carpark.of(result);
                }
                return null;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    public List<LiveParkingSpace> getLiveSpacesForDate(String date) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM liveParkingSpaces WHERE DATE(createdAt) = ? ORDER BY createdAt DESC");
            stmt.setString(1, date);

            try (ResultSet result = stmt.executeQuery()) {
                List<LiveParkingSpace> spaces = new ArrayList<>();

                while (result.next()) {
                    spaces.add(LiveParkingSpace.of(result));
                }
                return spaces;
            }
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public JsonArray getAllSpacesData() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT name, createdAt, spaces, status, open FROM liveParkingSpaces ORDER BY createdAt DESC"
            );

            try (ResultSet result = stmt.executeQuery()) {
                JsonArray array = new JsonArray();

                while (result.next()) {
                    JsonArray row = new JsonArray();
                    row.add(result.getString("createdAt"));
                    row.add(result.getString("name"));
                    row.add(result.getInt("spaces"));
                    row.add(result.getString("status"));
                    row.add(result.getBoolean("open"));

                    array.add(row);
                }
                return array;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new JsonArray();
        }
    }

    public List<String> getLiveSpacesDates() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT DISTINCT DATE_FORMAT(createdAt, '%Y-%m-%d') AS date FROM liveParkingSpaces ORDER BY date DESC");

            try (ResultSet result = stmt.executeQuery()) {
                List<String> dates = new ArrayList<>();

                while (result.next()) {
                    dates.add(result.getString("date"));
                }
                return dates;
            }
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public List<BusiestCarpark> getBusiestCarparks() {
        String sql = """
        SELECT 
            name,
            code,
            COUNT(*) AS timesFull
        FROM liveParkingSpaces
        WHERE spaces = 0 OR status = 'FULL'
        GROUP BY name, code
        ORDER BY timesFull DESC
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<BusiestCarpark> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new BusiestCarpark(
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getInt("timesFull")
                ));
            }
            return list;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<CarparkFullDay> getMostCommonFullDays() {
        String sql = """
        SELECT 
            name,
            code,
            DAYNAME(createdAt) AS dayOfWeek,
            COUNT(*) AS fullCount
        FROM liveParkingSpaces
        WHERE spaces = 0 OR status = 'FULL'
        GROUP BY name, code, dayOfWeek
        ORDER BY name, fullCount DESC
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<CarparkFullDay> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CarparkFullDay(
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getString("dayOfWeek"),
                        rs.getInt("fullCount")
                ));
            }
            return list;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<CarparkAvailability> getAvailabilityLastYear() {
        String sql = """
        SELECT 
            name,
            code,
            YEAR(createdAt) AS year,
            MONTH(createdAt) AS month,
            ROUND(100.0 * SUM(CASE WHEN spaces > 0 THEN 1 ELSE 0 END) / COUNT(*), 2) AS availabilityPercentage
        FROM liveParkingSpaces
        WHERE YEAR(createdAt) = YEAR(CURDATE()) - 1
        GROUP BY name, code, year, month
        ORDER BY name, year, month
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<CarparkAvailability> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CarparkAvailability(
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getDouble("availabilityPercentage")
                ));
            }
            return list;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<CarparkAvailability> getAvailabilityThisYear() {
        String sql = """
        SELECT 
            name,
            code,
            YEAR(createdAt) AS year,
            MONTH(createdAt) AS month,
            ROUND(100.0 * SUM(CASE WHEN spaces > 0 THEN 1 ELSE 0 END) / COUNT(*), 2) AS availabilityPercentage
        FROM liveParkingSpaces
        WHERE YEAR(createdAt) = YEAR(CURDATE())
        GROUP BY name, code, year, month
        ORDER BY name, year, month
        """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<CarparkAvailability> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CarparkAvailability(
                        rs.getString("name"),
                        rs.getString("code"),
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getDouble("availabilityPercentage")
                ));
            }
            return list;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ArrayList<>();
        }
    }

}
