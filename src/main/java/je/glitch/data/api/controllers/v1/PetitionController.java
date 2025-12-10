package je.glitch.data.api.controllers.v1;

import io.javalin.http.Context;
import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.ApiResponse;
import je.glitch.data.api.utils.ErrorType;
import je.glitch.data.api.utils.HttpException;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

@RequiredArgsConstructor
public class PetitionController {
    private final MySQLConnection connection;

    public void handleGetPetitions(Context ctx) {
        ctx.json(connection.getPetitionTable().getPetitions(ctx));
    }

    public void handleGetPetitionStats(Context ctx) {
        try {
            ctx.json(new ApiResponse<>(connection.getPetitionTable().getPetitionStats()));
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new HttpException(ErrorType.SERVER_ERROR, 500, "Failed to fetch petition stats");
        }
    }
}
