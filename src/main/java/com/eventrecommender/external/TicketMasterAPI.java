package com.eventrecommender.external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eventrecommender.entity.Item;
import com.eventrecommender.entity.Item.ItemBuilder;

/**
 * This class handles interactions with the TicketMaster API to fetch event data.
 */
public class TicketMasterAPI implements ExternalAPI {
    private static final String API_HOST = "app.ticketmaster.com";
    private static final String SEARCH_PATH = "/discovery/v2/events.json";
    private static final String RECOMMEND_PATH = "/discovery/v2/suggest.json";
    private static final String API_KEY = "vAiO3Wm87BQCgSAcmtDnprIEZudbFFD5"; // Replace with your actual API key

    /**
     * Sends an HTTP GET request to the TicketMaster API and processes the response.
     *
     * @param url   The base URL of the API endpoint.
     * @param query The query string parameters.
     * @return A list of events as `Item` objects.
     */
    private List<Item> sendRequestToTicketMaster(String url, String query) {
        try {
            // Establish an HTTP connection to the TicketMaster API
            HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + query).openConnection();
            connection.setRequestMethod("GET");

            // Log the request and response information
            System.out.println("\nSending 'GET' request to URL : " + url + "?" + query);
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code : " + responseCode);

            // Read the API response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the JSON response to extract events
            JSONObject responseJson = new JSONObject(response.toString());
            JSONObject embedded = (JSONObject) responseJson.get("_embedded");
            JSONArray events = (JSONArray) embedded.get("events");

            return getItemList(events);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Return null in case of an error
    }

    @Override
    public List<Item> getNearbyEvents(double lat, double lon) {
        // Construct the base URL for searching events near a specific location
        String url = "http://" + API_HOST + SEARCH_PATH;

        // Convert the latitude and longitude to a geohash with precision 4 (~20km)
        String geoHash = GeoHash.encodeGeohash(lat, lon, 4);

        // Build the query string with geohash and radius
        String query = String.format("apikey=%s&geoPoint=%s&radius=50", API_KEY, geoHash);
        System.out.println("Sending request for nearby events.");

        // Send the request and return the list of nearby events
        return sendRequestToTicketMaster(url, query);
    }

    @Override
    public List<Item> searchEventsByKeyword(double lat, double lon, String term) {
        // Construct the base URL for searching events by keyword
        String url = "http://" + API_HOST + RECOMMEND_PATH;

        // Convert the latitude and longitude to a geohash
        String geoHash = GeoHash.encodeGeohash(lat, lon, 4);

        // Encode the keyword to handle special characters in URLs
        term = urlEncodeHelper(term);

        // Build the query string with keyword, geohash, and radius
        String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=50", API_KEY, geoHash, term);
        System.out.println("Sending request with keyword: " + term);

        // Send the request and return the list of events matching the keyword
        return sendRequestToTicketMaster(url, query);
    }

    /**
     * Helper method to URL-encode a string for safe use in HTTP requests.
     */
    private String urlEncodeHelper(String url) {
        try {
            url = java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * Convert a JSON array of events into a list of `Item` objects.
     */
    private List<Item> getItemList(JSONArray events) throws JSONException {
        List<Item> itemList = new ArrayList<>();

        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            ItemBuilder builder = new ItemBuilder();
            builder.setItemId(getStringFieldOrNull(event, "id"));
            builder.setName(getStringFieldOrNull(event, "name"));
            builder.setDescription(getDescription(event));
            builder.setCategories(getCategories(event));
            builder.setDate(getStartDate(event));
            builder.setPriceRange(getPriceRange(event));
            builder.setImageUrl(getImageUrl(event));
            builder.setUrl(getStringFieldOrNull(event, "url"));

            // Extract venue details such as address, city, state, etc.
            JSONObject venue = getVenue(event);
            if (venue != null) {
                if (!venue.isNull("address")) {
                    JSONObject address = venue.getJSONObject("address");
                    StringBuilder sb = new StringBuilder();
                    if (!address.isNull("line1")) sb.append(address.getString("line1"));
                    if (!address.isNull("line2")) sb.append(address.getString("line2"));
                    if (!address.isNull("line3")) sb.append(address.getString("line3"));
                    builder.setAddress(sb.toString());
                }
                if (!venue.isNull("city")) {
                    JSONObject city = venue.getJSONObject("city");
                    builder.setCity(getStringFieldOrNull(city, "name"));
                }
                if (!venue.isNull("country")) {
                    JSONObject country = venue.getJSONObject("country");
                    builder.setCountry(getStringFieldOrNull(country, "name"));
                }
                if (!venue.isNull("state")) {
                    JSONObject state = venue.getJSONObject("state");
                    builder.setState(getStringFieldOrNull(state, "name"));
                }
                builder.setZipcode(getStringFieldOrNull(venue, "postalCode"));
                if (!venue.isNull("location")) {
                    JSONObject location = venue.getJSONObject("location");
                    builder.setLatitude(getNumericFieldOrNull(location, "latitude"));
                    builder.setLongitude(getNumericFieldOrNull(location, "longitude"));
                }
            }

            // Build and add the item to the list
            itemList.add(builder.build());
        }

        return itemList;
    }

    /**
     * Extracts the venue details from the event JSON object.
     */
    private JSONObject getVenue(JSONObject event) throws JSONException {
        if (!event.isNull("_embedded")) {
            JSONObject embedded = event.getJSONObject("_embedded");
            if (!embedded.isNull("venues")) {
                JSONArray venues = embedded.getJSONArray("venues");
                if (venues.length() >= 1) {
                    return venues.getJSONObject(0);
                }
            }
        }
        return null; // Return null if no venue information is available
    }

    /**
     * Extracts the image URL from the event JSON object.
     */
    private String getImageUrl(JSONObject event) throws JSONException {
        if (!event.isNull("images")) {
            JSONArray imagesArray = event.getJSONArray("images");
            if (imagesArray.length() >= 1) {
                return getStringFieldOrNull(imagesArray.getJSONObject(0), "url");
            }
        }
        return null; // Return null if no images are available
    }

    /**
     * Extracts the description from the event JSON object.
     */
    private String getDescription(JSONObject event) throws JSONException {
        if (!event.isNull("description")) return event.getString("description");
        if (!event.isNull("additionalInfo")) return event.getString("additionalInfo");
        if (!event.isNull("info")) return event.getString("info");
        if (!event.isNull("pleaseNote")) return event.getString("pleaseNote");
        return null; // Return null if no description is available
    }

    /**
     * Extracts categories (segments and genres) from the event JSON object.
     */
    private Set<String> getCategories(JSONObject event) throws JSONException {
        Set<String> categories = new HashSet<>();
        JSONArray classifications = event.getJSONArray("classifications");
        for (int j = 0; j < classifications.length(); j++) {
            JSONObject classification = classifications.getJSONObject(j);
            JSONObject segment = classification.getJSONObject("segment");
            categories.add(segment.getString("name"));
            if (!classification.isNull("genre")) {
                JSONObject genre = classification.getJSONObject("genre");
                categories.add(genre.getString("name"));
            }
        }
        return categories;
    }

    /**
     * Extracts the start date of the event.
     */
    private String getStartDate(JSONObject event) throws JSONException {
        JSONObject dates = event.getJSONObject("dates");
        JSONObject start = dates.getJSONObject("start");
        return start.getString("localDate");
    }

    /**
     * Extracts the price range of the event.
     */
    private String getPriceRange(JSONObject event) throws JSONException {
        if (event.isNull("priceRanges")) return "NA";
        JSONObject priceRanges = event.getJSONArray("priceRanges").getJSONObject(0);
        int min = priceRanges.getInt("min");
        int max = priceRanges.getInt("max");
        return "$" + min + "-" + max;
    }

    /**
     * Extracts a string field from a JSON object or returns null if not available.
     */
    private String getStringFieldOrNull(JSONObject event, String field) throws JSONException {
        return event.isNull(field) ? null : event.getString(field);
    }

    /**
     * Extracts a numeric field from a JSON object or returns 0.0 if not available.
     */
    private double getNumericFieldOrNull(JSONObject event, String field) throws JSONException {
        return event.isNull(field) ? 0.0 : event.getDouble(field);
    }
}
