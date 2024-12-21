package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
       
    /**
     * Default constructor for ItemHistory servlet.
     */
    public ItemHistory() {
        super();
    }

	/**
	 * Handles GET requests to fetch a user's favorite items.
	 * @param request  HTTP request containing the user_id parameter.
	 * @param response HTTP response to send back the list of favorite items in JSON format.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Retrieve user ID from request parameters
		String userId = request.getParameter("user_id");
		JSONArray array = new JSONArray(); // To store the resulting favorite items in JSON format

		// Connect to the database
		DBConnection conn = DBConnectionFactory.getDBConnection();
		Set<Item> items = conn.getFavoriteItems(userId); // Fetch favorite items for the user

		// Convert each item to a JSON object and mark it as a favorite
		for (Item item : items) {
			JSONObject obj = item.toJSONObject();
			try {
				obj.append("favorite", true); // Indicate the item is a favorite
			} catch (JSONException e) {
				e.printStackTrace();
			}
			array.put(obj); // Add the JSON object to the response array
		}

		// Write the JSON array back to the response
		RpcHelper.writeJsonArray(response, array);
	}

	/**
	 * Handles OPTIONS requests to handle preflight checks for CORS (Cross-Origin Resource Sharing).
	 * @param req  HTTP request object.
	 * @param resp HTTP response object.
	 */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Set CORS headers to allow cross-origin requests
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
	}
	
	/**
	 * Handles POST requests to add favorite items for a user.
	 * @param request  HTTP request containing user_id and a list of item IDs to add as favorites.
	 * @param response HTTP response confirming the operation was successful.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			// Read the input JSON object from the request
			JSONObject input = RpcHelper.readJsonObject(request);
			String userId = input.getString("user_id"); // Extract user ID
			JSONArray array = (JSONArray) input.get("favorite"); // Extract the list of favorite items

			// Convert the JSONArray to a List of item IDs
			List<String> histories = new ArrayList<>();
			for (int i = 0; i < array.length(); i++) {
				String itemId = (String) array.get(i);
				histories.add(itemId);
			}

			// Connect to the database and set the favorite items for the user
			DBConnection conn = DBConnectionFactory.getDBConnection();
			conn.setFavoriteItems(userId, histories);

			// Respond with a success message
			RpcHelper.writeJsonObject(response, new JSONObject().put("result", "SUCCESS"));
		} catch (JSONException e) {
			e.printStackTrace(); // Log the error
		}
	}
	
	/**
	 * Handles DELETE requests to remove favorite items for a user.
	 * @param request  HTTP request containing user_id and a list of item IDs to remove from favorites.
	 * @param response HTTP response confirming the operation was successful.
	 */
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			// Read the input JSON object from the request
			JSONObject input = RpcHelper.readJsonObject(request);
			String userId = input.getString("user_id"); // Extract user ID
			JSONArray array = (JSONArray) input.get("favorite"); // Extract the list of favorite items

			// Convert the JSONArray to a List of item IDs
			List<String> histories = new ArrayList<>();
			for (int i = 0; i < array.length(); i++) {
				String itemId = (String) array.get(i);
				histories.add(itemId);
			}
 			
			// Connect to the database and unset (remove) the favorite items for the user
			DBConnection conn = DBConnectionFactory.getDBConnection();
			conn.unsetFavoriteItems(userId, histories);

			// Respond with a success message
			RpcHelper.writeJsonObject(response, new JSONObject().put("result", "SUCCESS"));
		} catch (JSONException e) {
			e.printStackTrace(); // Log the error
		}
	}
}