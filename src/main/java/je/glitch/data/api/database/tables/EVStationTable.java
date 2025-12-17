package je.glitch.data.api.database.tables;

import com.google.gson.JsonArray;
import com.zaxxer.hikari.HikariDataSource;
import je.glitch.data.api.models.EVStation;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class EVStationTable implements ITable {
    private final HikariDataSource dataSource;

    public List<EVStation> getStationsWithFullInfo() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM evStations ORDER BY createdAt DESC"
            );

            try (ResultSet result = stmt.executeQuery()) {
                List<EVStation> output = new ArrayList<>();

                while (result.next()) {
                   output.add(EVStation.of(result));
                }
                return output;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

}
