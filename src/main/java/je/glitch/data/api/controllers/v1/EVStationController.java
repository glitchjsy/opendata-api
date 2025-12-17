package je.glitch.data.api.controllers.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.javalin.http.Context;
import je.glitch.data.api.models.ApiResponse;
import je.glitch.data.api.services.CarparkService;
import je.glitch.data.api.services.EVStationService;
import je.glitch.data.api.utils.ErrorResponse;
import je.glitch.data.api.utils.ErrorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EVStationController {
    private static final Gson compactGson = new GsonBuilder().create();
    private final EVStationService service;

    public void handleGetStations(Context ctx) {
        ctx.json(new ApiResponse<>(service.getAllStations()));
    }
}
