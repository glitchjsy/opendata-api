package je.glitch.data.api.services;

import je.glitch.data.api.cache.RedisCache;
import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.EVStation;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class EVStationService {
    private final MySQLConnection connection;

    public List<EVStation> getAllStations() {
        return connection.getEvStationTable().getStationsWithFullInfo();
    }
}
