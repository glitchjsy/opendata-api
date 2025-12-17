package je.glitch.data.api.database;

import com.zaxxer.hikari.HikariDataSource;
import je.glitch.data.api.Config;
import je.glitch.data.api.database.tables.*;
import lombok.Getter;

@Getter
public class MySQLConnection {
    private final HikariDataSource dataSource = new HikariDataSource();
    private final CarparkTable carparkTable;
    private final VehicleTable vehicleTable;
    private final BusTable busTable;
    private final ApiKeyTable apiKeyTable;
    private final UserTable userTable;
    private final LogTable logTable;
    private final FoiTable foiTable;
    private final CourtTable courtTable;
    private final PetitionTable petitionTable;
    private final EVStationTable evStationTable;

    public MySQLConnection() {
        this.connect();
        this.carparkTable = new CarparkTable(dataSource);
        this.vehicleTable = new VehicleTable(dataSource);
        this.busTable = new BusTable(dataSource);
        this.apiKeyTable = new ApiKeyTable(dataSource);
        this.userTable = new UserTable(dataSource);
        this.logTable = new LogTable(dataSource);
        this.foiTable = new FoiTable(dataSource);
        this.courtTable = new CourtTable(dataSource);
        this.petitionTable = new PetitionTable(dataSource);
        this.evStationTable = new EVStationTable(dataSource);
    }

    private void connect() {
        dataSource.setJdbcUrl("jdbc:mysql://localhost/" + Config.getMysqlDatabase());
        dataSource.setUsername("root");
        dataSource.setPassword(Config.getMysqlPassword());
        dataSource.setMaxLifetime(1800000);
    }
}
