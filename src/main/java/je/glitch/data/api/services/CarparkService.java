package je.glitch.data.api.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import je.glitch.data.api.cache.RedisCache;
import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.Carpark;

import je.glitch.data.api.models.LiveParkingSpace;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class CarparkService {
    private final MySQLConnection connection;
    private final RedisCache cache;

    public List<Carpark> getAllCarparks() {
        return connection.getCarparkTable().getCarparks();
    }

    public Carpark getCarparkByIdOrCode(String idOrCode) {
        try {
            UUID.fromString(idOrCode);
            return connection.getCarparkTable().getCarparkById(idOrCode);
        } catch (IllegalArgumentException ex) {
            return connection.getCarparkTable().getCarparkByLiveTrackingCode(idOrCode);
        }
    }

    public List<Map<String, Object>> getLiveSpaces(boolean includeCarparkInfo) {
        List<Map<String, Object>> spaces = cache.getLiveParkingSpaces()
                .stream()
                .map(space -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", space.getName());
                    map.put("code", space.getCode());
                    map.put("spaces", space.getSpaces());
                    map.put("status", space.getStatus());
                    map.put("open", space.isOpen());
                    return map;
                })
                .toList();

        if (!includeCarparkInfo) {
            return spaces;
        }

        return spaces.stream()
                .map(space -> {
                    Carpark carparkInfo = connection.getCarparkTable().getCarparkByLiveTrackingCode(space.get("code").toString());
                    Map<String, Object> map = new HashMap<>(space);
                    map.put("carparkInfo", carparkInfo);
                    return map;
                })
                .toList();
    }

    public List<LiveParkingSpace> getLiveSpacesForDate(String date) {
        return connection.getCarparkTable().getLiveSpacesForDate(date);
    }

    public List<String> getLiveSpacesDates() {
        return connection.getCarparkTable().getLiveSpacesDates();
    }

    public Map<String, Object> getParkingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("busiestCarparks", connection.getCarparkTable().getBusiestCarparks());
        stats.put("mostCommonFullDays", connection.getCarparkTable().getMostCommonFullDays());
        stats.put("availabilityLastYear", connection.getCarparkTable().getAvailabilityLastYear());
        stats.put("availabilityThisYear", connection.getCarparkTable().getAvailabilityThisYear());
        return stats;
    }

    public JsonArray getAllSpacesData() {
        return connection.getCarparkTable().getAllSpacesData();
    }
}
