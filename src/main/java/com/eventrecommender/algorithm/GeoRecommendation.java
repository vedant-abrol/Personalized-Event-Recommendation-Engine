package com.eventrecommender.algorithm;

import java.util.*;

import com.eventrecommender.db.mongodb.MongoDBConnection;
import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.entity.Item;
import com.eventrecommender.external.ExternalAPI;
import com.eventrecommender.external.ExternalAPIFactory;

public class GeoRecommendation {

    // Initialize MongoDB connection for user and item data retrieval
    private final DBConnection conn = MongoDBConnection.getInstance();

    // Initialize TicketMaster API for external event search
    private final ExternalAPI ticketMasterAPI = ExternalAPIFactory.getExternalAPI();

    /**
     * Recommend events for the user based on their favorite items and current location.
     * 
     * @param userId The ID of the user for whom recommendations are generated.
     * @param lat    The latitude of the user's current location.
     * @param lon    The longitude of the user's current location.
     * @return A list of recommended events (items) sorted by proximity to the user's location.
     */
    public List<Item> recommendItems(String userId, double lat, double lon) {
        // Step 1: Fetch favorite item IDs for the user
        Set<String> favoriteItemIds = conn.getFavoriteItemIds(userId);
        if (favoriteItemIds == null || favoriteItemIds.isEmpty()) {
            // If the user has no favorite items, return an empty list
            System.out.println("No favorite items found for user: " + userId);
            return new ArrayList<>();
        }

        // Step 2: Fetch categories associated with the user's favorite items
        Set<String> favoriteCategories = new HashSet<>();
        for (String itemId : favoriteItemIds) {
            Set<String> categories = conn.getCategories(itemId);
            if (categories != null) {
                favoriteCategories.addAll(categories);
            }
        }

        // Remove undefined categories and handle empty categories
        favoriteCategories.remove("Undefined");
        if (favoriteCategories.isEmpty()) {
            favoriteCategories.add(""); // Default to empty keyword if no valid categories
        }

        // Step 3: Use TicketMaster API to fetch events matching favorite categories
        Set<Item> recommendedItems = new HashSet<>();
        for (String category : favoriteCategories) {
            // Search for events in the given category near the user's location
            List<Item> items = ticketMasterAPI.searchEventsByKeyword(lat, lon, category);
            if (items != null) { // Ensure valid results before adding to recommendations
                recommendedItems.addAll(items);
            }
        }

        // Step 4: Exclude items the user has already marked as favorite
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : recommendedItems) {
            if (item != null && !favoriteItemIds.contains(item.getItemId())) {
                filteredItems.add(item);
            }
        }

        // Step 5: Rank the events by distance from the user's current location
        filteredItems.sort(Comparator.comparingDouble(item -> 
            getDistance(item.getLatitude(), item.getLongitude(), lat, lon)
        ));

        return filteredItems;
    }

    /**
     * Calculate the distance between two geographic points using the Haversine formula.
     * 
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The distance between the two points in miles.
     */
    private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitudes and longitudes to radians
        double dlon = Math.toRadians(lon2 - lon1);
        double dlat = Math.toRadians(lat2 - lat1);

        // Haversine formula to calculate distance
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double R = 3961; // Radius of Earth in miles

        return R * c; // Distance in miles
    }
}
