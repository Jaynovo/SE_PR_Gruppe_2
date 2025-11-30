package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.Address;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoCodingService {

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Address enrichWithCoordinates(Address address) {
        try {
            String query = String.format("%s %s %s %s",
                    address.getStreet(), address.getHouseNumber(),
                    address.getPostalCode(), address.getCity());

            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                    query.replace(" ", "+");

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "JavaFX SmartHomeApp")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                JsonNode node = mapper.readTree(response.body().string());
                if (node.size() > 0) {
                    address.setLatitude(node.get(0).get("lat").asDouble());
                    address.setLongitude(node.get(0).get("lon").asDouble());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }
}
