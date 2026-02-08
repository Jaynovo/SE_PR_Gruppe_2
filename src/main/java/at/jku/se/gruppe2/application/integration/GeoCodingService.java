package at.jku.se.gruppe2.application.integration;

import at.jku.se.gruppe2.domain.model.home.Address;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


/**
 * Utility service for enriching an {@link Address} with geographic coordinates (latitude/longitude)
 * by performing a forward-geocoding request against the OpenStreetMap Nominatim API.
 *
 * <p><b>Behavior and error handling:</b>
 * <ul>
 *   <li>If {@code address} is {@code null}, the method returns immediately.</li>
 *   <li>If the HTTP response is not {@code 200 OK}, no coordinates are set.</li>
 *   <li>If the response contains no results or lacks {@code lat}/{@code lon}, no coordinates are set.</li>
 *   <li>All exceptions are caught internally; the method does not throw.</li>
 * </ul>
 *
 * <p><b>Side effects:</b> writes diagnostic output to {@code System.out}/{@code System.err}.
 *
 * <p><b>External dependency:</b> calls {@code https://nominatim.openstreetmap.org/}.
 */
public class GeoCodingService {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();


    /**
     * Enriches the given {@link Address} instance with latitude and longitude by querying
     * the OpenStreetMap Nominatim API using the address fields (street, house number,
     * postal code, city, country).
     *
     * <p>If geocoding succeeds, {@link Address#setLatitude(double)} and
     * {@link Address#setLongitude(double)} are called on the provided instance.</p>
     *
     * <p>This method is intentionally fail-safe: it catches all exceptions and prints
     * error information instead of throwing.</p>
     *
     * @param address the address to enrich; if {@code null}, the method returns without changes
     * @return nothing (void)
     * @throws none (all exceptions are caught internally)
     */
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

    /**
     * Converts {@code null} strings to the empty string to avoid {@code null} values
     * in formatted query text.
     *
     * @param s the input string (may be {@code null})
     * @return {@code ""} if {@code s == null}, otherwise {@code s}
     */
    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
