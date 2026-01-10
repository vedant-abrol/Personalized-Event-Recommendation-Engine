package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.eventrecommender.algorithm.GeoRecommendation;
import com.eventrecommender.entity.Item;
import com.eventrecommender.external.ExternalAPI;
import com.eventrecommender.external.ExternalAPIFactory;

/**
 * Servlet to handle recommendations for users based on their preferences and location.
 * This servlet interacts with the GeoRecommendation class to fetch personalized event recommendations.
 */
@WebServlet("/recommendation")
public class RecommendItem extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RecommendItem.class.getName());
       
    /**
     * Default constructor for RecommendItem servlet.
     */
    public RecommendItem() {
        super();
    }

    /**
     * Handles GET requests to provide event recommendations for the user.
     * @param request  HTTP request containing user ID, latitude, and longitude.
     * @param response HTTP response to send back the recommended events in JSON format.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("user_id");
        double lat = Double.parseDouble(request.getParameter("lat"));
        double lon = Double.parseDouble(request.getParameter("lon"));
        
        List<Item> items = new ArrayList<>();
        
        try {
            // Try to get personalized recommendations using GeoRecommendation
            GeoRecommendation recommendation = new GeoRecommendation();
            items = recommendation.recommendItems(userId, lat, lon);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "GeoRecommendation failed, falling back to direct API call", e);
        }
        
        // If no recommendations from GeoRecommendation, try using in-memory favorites
        if (items == null || items.isEmpty()) {
            Set<String> inMemoryFavorites = ItemHistory.getInMemoryFavorites(userId);
            if (!inMemoryFavorites.isEmpty()) {
                LOGGER.info("Using in-memory favorites for recommendations");
                // For now, just return nearby events as recommendations
                // In a full implementation, we'd search by favorite categories
            }
            
            // Fallback: return nearby events as "recommendations"
            LOGGER.info("Falling back to nearby events as recommendations");
            try {
                ExternalAPI api = ExternalAPIFactory.getExternalAPI();
                items = api.getNearbyEvents(lat, lon);
                if (items == null) {
                    items = new ArrayList<>();
                }
            } catch (Exception apiEx) {
                LOGGER.log(Level.SEVERE, "Failed to fetch events from API", apiEx);
                items = new ArrayList<>();
            }
        }

        // Convert the recommended items into a JSON array
        JSONArray result = new JSONArray();
        try {
            for (Item item : items) {
                result.put(item.toJSONObject());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error converting items to JSON", e);
        }

        // Write the JSON array to the HTTP response
        RpcHelper.writeJsonArray(response, result);
    }

    /**
     * Handles POST requests by delegating to the GET method.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
