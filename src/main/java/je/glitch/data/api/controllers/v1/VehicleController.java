package je.glitch.data.api.controllers.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;
import je.glitch.data.api.models.ApiResponse;
import je.glitch.data.api.models.Vehicle;
import je.glitch.data.api.services.VehicleService;
import je.glitch.data.api.utils.ErrorResponse;
import je.glitch.data.api.utils.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class VehicleController {
    private static final Gson compactGson = new GsonBuilder().create();
    private final VehicleService service;

    public void handleGetVehicles(Context ctx) {
        service.getVehicles(ctx);
    }

    public void handleGetStats(Context ctx) throws SQLException {
        Map<String, Object> stats = service.getStats(ctx);
        ctx.json(new ApiResponse<>(stats));
    }

    public void handleGetColors(Context ctx) throws SQLException {
        Map<String, Object> stats = service.getColors(ctx);
        ctx.json(new ApiResponse<>(stats));
    }

    public void handleGetMakes(Context ctx) throws SQLException {
        Map<String, Object> stats = service.getMakes(ctx);
        ctx.json(new ApiResponse<>(stats));
    }

    public void handleGetModels(Context ctx) throws SQLException {
        Map<String, Object> stats = service.getModels(ctx);
        ctx.json(stats);
    }

    public void handleGetPlate(Context ctx) {
        try {
            String plate = ctx.pathParam("plate");
            JsonObject vehicleData = service.getPlate(plate);

            if (vehicleData == null) {
                ctx.status(400).json(new ErrorResponse(ErrorType.INVALID_REQUEST, "Invalid plate"));
                return;
            }
            ctx.json(new ApiResponse<>(vehicleData));
        } catch (Exception ex) {
            log.error("An error occurred while fetching plate information", ex);
            ctx.status(500).json(new ErrorResponse(ErrorType.SERVER_ERROR, "An error has occurred"));
        }
    }
}
