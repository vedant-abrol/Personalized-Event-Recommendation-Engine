package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.db.mysql.DBConnectionFactory;
import com.eventrecommender.entity.Item;

/**
 * Servlet to handle user favorite event history.
 * This includes fetching, adding, and removing favorite events for a user.
 */
@WebServlet("/history")
public class ItemHistory extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ItemHistory.class.getName());
    
    // In-memory fallback store for favorites when database is unavailable
    private static final Map<String, Set<String>> inMemoryFavorites = new ConcurrentHashMap<>();
    
    // In-memory cache for item data (used when DB is unavailable)
    private static final Map<String, Item> itemCache = new ConcurrentHashMap<>();
       
    /**
     * Default constructor for ItemHistory servlet.
     */
    public ItemHistory() {
        super();
    }
    
    /**
     * Gets the in-memory favorites for a user (used as fallback when DB is unavailable)
     */
    public static Set<String> getInMemoryFavorites(String userId) {
        return inMemoryFavorites.getOrDefault(userId, new HashSet<>());
    }
    
    /**
     * Cache an item for later retrieval (called from SearchItem when items are loaded)
     */
    public static void cacheItem(Item item) {
        if (item != null && item.getItemId() != null) {
            itemCache.put(item.getItemId(), item);
        }
    }
    
    /**
     * Cache multiple items
     */
    public static void cacheItems(List<Item> items) {
        if (items != null) {
            for (Item item : items) {
                cacheItem(item);
            }
        }
    }
    
    /**
     * Get a cached item by ID
     */
    public static Item getCachedItem(String itemId) {
        return itemCache.get(itemId);
    }

    /**
     * Handles GET requests to fetch a user's favorite items.
     * @param request  HTTP request containing the user_id parameter.
     * @param response HTTP response to send back the list of favorite items in JSON format.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("user_id");
        JSONArray array = new JSONArray();

        boolean dbWorked = false;
        try {
            DBConnection conn = DBConnectionFactory.getDBConnection();
            Set<Item> items = conn.getFavoriteItems(userId);
            dbWorked = true;

            for (Item item : items) {
                JSONObject obj = item.toJSONObject();
                try {
                    obj.put("favorite", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                array.put(obj);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database unavailable, using in-memory cache for favorites", e);
        }
        
        // If DB didn't work, use in-memory cache
        if (!dbWorked) {
            Set<String> favoriteIds = inMemoryFavorites.getOrDefault(userId, new HashSet<>());
            for (String itemId : favoriteIds) {
                Item cachedItem = itemCache.get(itemId);
                if (cachedItem != null) {
                    try {
                        JSONObject obj = cachedItem.toJSONObject();
                        obj.put("favorite", true);
                        array.put(obj);
                    } catch (JSONException e) {
                        LOGGER.log(Level.WARNING, "Error converting cached item to JSON", e);
                    }
                }
            }
        }

        RpcHelper.writeJsonArray(response, array);
    }

    /**
     * Handles OPTIONS requests to handle preflight checks for CORS (Cross-Origin Resource Sharing).
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
    
    /**
     * Handles POST requests to add favorite items for a user.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JSONObject input = RpcHelper.readJsonObject(request);
            String userId = input.getString("user_id");
            JSONArray array = (JSONArray) input.get("favorite");

            List<String> histories = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                String itemId = (String) array.get(i);
                histories.add(itemId);
            }

            try {
                DBConnection conn = DBConnectionFactory.getDBConnection();
                conn.setFavoriteItems(userId, histories);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Database unavailable, storing favorite in memory only", e);
            }
            
            // Always store in memory as backup
            inMemoryFavorites.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).addAll(histories);

            RpcHelper.writeJsonObject(response, new JSONObject().put("result", "SUCCESS"));
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error processing favorite request", e);
            try {
                RpcHelper.writeJsonObject(response, new JSONObject().put("result", "ERROR"));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Handles DELETE requests to remove favorite items for a user.
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JSONObject input = RpcHelper.readJsonObject(request);
            String userId = input.getString("user_id");
            JSONArray array = (JSONArray) input.get("favorite");

            List<String> histories = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                String itemId = (String) array.get(i);
                histories.add(itemId);
            }

            try {
                DBConnection conn = DBConnectionFactory.getDBConnection();
                conn.unsetFavoriteItems(userId, histories);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Database unavailable, removing favorite from memory only", e);
            }
            
            // Always remove from memory as well
            Set<String> userFavorites = inMemoryFavorites.get(userId);
            if (userFavorites != null) {
                userFavorites.removeAll(histories);
            }

            RpcHelper.writeJsonObject(response, new JSONObject().put("result", "SUCCESS"));
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error processing unfavorite request", e);
            try {
                RpcHelper.writeJsonObject(response, new JSONObject().put("result", "ERROR"));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }
}
