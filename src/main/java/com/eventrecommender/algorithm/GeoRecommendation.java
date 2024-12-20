package com.eventrecommender.algorithm;

import java.util.*;

import com.eventrecommender.db.mongodb.MongoDBConnection;
import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.entity.Item;
import com.eventrecommender.external.ExternalAPI;
import com.eventrecommender.external.ExternalAPIFactory;

public class GeoRecommendation {

    private final DBConnection conn = MongoDBConnection.getInstance();
    private final ExternalAPI ticketMasterAPI = ExternalAPIFactory.getExternalAPI();

    /**
     * Recommend events for the user based on their favorite items and current location.
     */
    public List<Item> recommendItems(String userId, double lat, double lon) {
        // Step 1: Fetch favorite item IDs for the user
        Set<String> favoriteItemIds = conn.getFavoriteItemIds(userId);
        if (favoriteItemIds == null || favoriteItemIds.isEmpty()) {
            System.out.println("No favorite items found for user: " + userId);
            return new ArrayList<>(); // Return empty list if no favorite items
        }
    
        // Step 2: Fetch categories associated with favorite items
        Set<String> favoriteCategories = new HashSet<>();
        for (String itemId : favoriteItemIds) {
            Set<String> categories = conn.getCategories(itemId);
            if (categories != null) {
                favoriteCategories.addAll(categories);
            }
        }
    
        // Handle case where categories are empty or undefined
        favoriteCategories.remove("Undefined");
        if (favoriteCategories.isEmpty()) {
            favoriteCategories.add(""); // Default to empty keyword
        }
    
        // Step 3: Fetch events from TicketMasterAPI based on favorite categories
        Set<Item> recommendedItems = new HashSet<>();
        for (String category : favoriteCategories) {
            // Use TicketMaster API to fetch events based on keyword/category
            List<Item> items = ticketMasterAPI.searchEventsByKeyword(lat, lon, category);
            if (items != null) { // Ensure items are not null before adding
                recommendedItems.addAll(items);
            }
        }
    
        // Step 4: Exclude already favorited items
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
     * Calculate distance between two latitude/longitude points using Haversine formula.
     */
    private static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double dlon = Math.toRadians(lon2 - lon1);
        double dlat = Math.toRadians(lat2 - lat1);
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double R = 3961; // Earth's radius in miles
        return R * c;
    }
}
