package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.json.JSONObject;

import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.db.mysql.DBConnectionFactory;
import com.eventrecommender.entity.Item;
import com.eventrecommender.external.ExternalAPI;
import com.eventrecommender.external.ExternalAPIFactory;
import com.eventrecommender.rpc.ItemHistory;

/**
 * Servlet implementation class SearchItem
 * 
 * Handles HTTP GET and POST requests to search for nearby events based on the
 * user's latitude and longitude.
 */
@WebServlet("/search")
public class SearchItem extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SearchItem.class.getName());

    /**
     * Default constructor.
     */
    public SearchItem() {
        super();
    }

    /**
     * Handles HTTP GET requests.
     *
     * @param request  The `HttpServletRequest` object containing client request
     *                 data.
     * @param response The `HttpServletResponse` object to send data back to the
     *                 client.
     * @throws ServletException If an exception occurs during request processing.
     * @throws IOException      If an I/O error occurs during request processing.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("user_id");
        String category = request.getParameter("category");

        Double lat = parseCoordinate(request.getParameter("lat"));
        Double lon = parseCoordinate(request.getParameter("lon"));

        if (lat == null || lon == null) {
            RpcHelper.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid latitude or longitude supplied.");
            return;
        }

        try {
            List<Item> items = null;
            Set<String> favorite = new HashSet<>();
            
            // Check if category filter is applied (not null, not empty, not "All")
            boolean hasCategoryFilter = category != null && !category.isEmpty() && !category.equalsIgnoreCase("All");
            
            // If category filter is applied, always use TicketMaster API directly for accurate filtering
            if (hasCategoryFilter) {
                LOGGER.info("Category filter applied: " + category + " - calling TicketMaster API directly");
                ExternalAPI api = ExternalAPIFactory.getExternalAPI();
                items = api.getNearbyEvents(lat, lon, category);
                if (items == null) {
                    items = new ArrayList<>();
                }
                favorite = ItemHistory.getInMemoryFavorites(userId);
            } else {
                // No category filter - try DB first, then fall back to API
                try {
                    DBConnection conn = DBConnectionFactory.getDBConnection();
                    if (conn != null) {
                        items = conn.searchItems(lat, lon);
                        favorite = conn.getFavoriteItemIds(userId);
                        if (favorite == null) {
                            favorite = new HashSet<>();
                        }
                    }
                } catch (Exception dbEx) {
                    LOGGER.log(Level.WARNING, "Database connection failed, falling back to direct API call", dbEx);
                }
                
                // If DB call failed or returned null, call TicketMaster API directly
                if (items == null || items.isEmpty()) {
                    LOGGER.info("Fetching events directly from TicketMaster API (no category filter)");
                    ExternalAPI api = ExternalAPIFactory.getExternalAPI();
                    items = api.getNearbyEvents(lat, lon, category);
                    if (items == null) {
                        items = new ArrayList<>();
                    }
                    // Use in-memory favorites as fallback
                    favorite = ItemHistory.getInMemoryFavorites(userId);
                }
            }
            
            // Cache items for later use in favorites display
            ItemHistory.cacheItems(items);

            List<JSONObject> list = new ArrayList<>();
            for (Item item : items) {
                JSONObject obj = item.toJSONObject();
                obj.put("favorite", favorite.contains(item.getItemId()));
                list.add(obj);
            }

            JSONArray array = new JSONArray(list);
            RpcHelper.writeJsonArray(response, array);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while searching items", e);
            RpcHelper.writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to process your request right now.");
        }
    }

    /**
     * Handles HTTP POST requests by delegating to `doGet`.
     *
     * @param request  The `HttpServletRequest` object containing client request
     *                 data.
     * @param response The `HttpServletResponse` object to send data back to the
     *                 client.
     * @throws ServletException If an exception occurs during request processing.
     * @throws IOException      If an I/O error occurs during request processing.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response); // Reuse the logic in `doGet`
    }

    private Double parseCoordinate(String coordinate) {
        try {
            return Double.parseDouble(coordinate);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse coordinate: {0}", coordinate);
            return null;
        }
    }
}
