package at.jku.se.gruppe2.service;

import at.jku.se.gruppe2.model.Address;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GeoCodingService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void enrichWithCoordinates(Address address) {
        try {
            if (address == null) {
                System.err.println("GeoCodingService: address is null");
                return;
            }

            String query = String.format(
                    "%s %s, %s %s, %s",
                    nullSafe(address.getStreet()),
                    nullSafe(address.getHouseNumber()),
                    nullSafe(address.getPostalCode()),
                    nullSafe(address.getCity()),
                    nullSafe(address.getCountry())
            );

            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(
                    Locale.US,
                    "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1",
                    encoded
            );

            System.out.println("GeoCodingService: query = " + query);
            System.out.println("GeoCodingService: calling " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SmartHomeSimulator/1.0 (markus.gaber@gmx.at)")
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("GeoCodingService: HTTP status = " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("GeoCodingService: non-200 response: " + response.body());
                return;
            }

            String body = response.body();
            // System.out.println("GeoCodingService response: " + body);

            JsonNode root = mapper.readTree(body);

            if (!root.isArray() || root.size() == 0) {
                System.err.println("GeoCodingService: no results for address");
                System.err.println("Body was: " + body);
                return;
            }

            JsonNode first = root.get(0);

            if (!first.has("lat") || !first.has("lon")) {
                System.err.println("GeoCodingService: 'lat' or 'lon' not found in result");
                System.err.println("First result: " + first);
                return;
            }

            double lat = first.get("lat").asDouble();
            double lon = first.get("lon").asDouble();

            System.out.println("GeoCodingService: parsed lat=" + lat + ", lon=" + lon);

            //Koordinaten in Address
            address.setLatitude(lat);
            address.setLongitude(lon);

        } catch (Exception e) {
            System.err.println("GeoCodingService: exception while geocoding");
            e.printStackTrace();
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
