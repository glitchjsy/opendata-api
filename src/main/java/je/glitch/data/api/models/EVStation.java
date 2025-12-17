package je.glitch.data.api.models;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Data
public class EVStation {

    private final int id;
    private final Timestamp createdAt;
    private final String name;
    private final String address;
    private final String parish;
    private final double latitude;
    private final double longitude;
    private final String provider;
    private final String operatorName;
    private final boolean rapid;

    public static EVStation of(ResultSet result) throws SQLException {
        return new EVStation(
                result.getInt("id"),
                result.getTimestamp("createdAt"),
                result.getString("name"),
                result.getString("address"),
                result.getString("parish"),
                result.getDouble("latitude"),
                result.getDouble("longitude"),
                result.getString("provider"),
                result.getString("operatorName"),
                result.getBoolean("rapid")
        );
    }
}
