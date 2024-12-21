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
import org.json.JSONObject;

import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.db.mysql.DBConnectionFactory;
import com.eventrecommender.entity.Item;

/**
 * Servlet implementation class SearchItem
 * 
 * Handles HTTP GET and POST requests to search for nearby events based on the
 * user's latitude and longitude.
 */
@WebServlet("/search")
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		// Step 1: Extract parameters from the HTTP request
		String userId = request.getParameter("user_id"); // User ID
		double lat = Double.parseDouble(request.getParameter("lat")); // Latitude
		double lon = Double.parseDouble(request.getParameter("lon")); // Longitude

		// Step 2: Establish database connection
		DBConnection conn = DBConnectionFactory.getDBConnection();
		if (conn == null) {
			// Handle case where database connection is not configured correctly
			throw new ServletException("DBConnection is null. Check database connection configuration.");
		}

		// Step 3: Search for nearby items using the provided latitude and longitude
		List<Item> items = conn.searchItems(lat, lon);

		// Step 4: Get the list of favorite item IDs for the user
		Set<String> favorite = conn.getFavoriteItemIds(userId);

		// Step 5: Prepare the response JSON array
		List<JSONObject> list = new ArrayList<>();
		try {
			for (Item item : items) {
				// Convert the `Item` object to a JSON object
				JSONObject obj = item.toJSONObject();

				// Check if the item is in the user's favorites and add the "favorite" flag
				if (favorite != null) {
					obj.put("favorite", favorite.contains(item.getItemId()));
				}

				// Add the JSON object to the list
				list.add(obj);
			}
		} catch (Exception e) {
			// Handle any exceptions during JSON processing
			e.printStackTrace();
		}

		// Step 6: Write the response as a JSON array
		JSONArray array = new JSONArray(list);
		RpcHelper.writeJsonArray(response, array);
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
}
