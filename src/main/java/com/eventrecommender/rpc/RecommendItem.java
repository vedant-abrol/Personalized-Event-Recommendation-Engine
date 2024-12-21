package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import com.eventrecommender.algorithm.GeoRecommendation;
import com.eventrecommender.entity.Item;

/**
 * Servlet to handle recommendations for users based on their preferences and location.
 * This servlet interacts with the GeoRecommendation class to fetch personalized event recommendations.
 */
@WebServlet("/recommendation")
public class RecommendItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
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
		// Retrieve user ID, latitude, and longitude from the request parameters
		String userId = request.getParameter("user_id");
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		
		// Use GeoRecommendation to generate recommendations
		GeoRecommendation recommendation = new GeoRecommendation();
		List<Item> items = recommendation.recommendItems(userId, lat, lon);

		// Convert the recommended items into a JSON array
		JSONArray result = new JSONArray();
		try {
			for (Item item : items) {
				result.put(item.toJSONObject());
			}
		} catch (Exception e) {
			e.printStackTrace(); // Log any exceptions
		}

		// Write the JSON array to the HTTP response
		RpcHelper.writeJsonArray(response, result);
	}

	/**
	 * Handles POST requests by delegating to the GET method.
	 * This allows the same functionality for both GET and POST requests.
	 * @param request  HTTP request object.
	 * @param response HTTP response object.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Delegate to doGet for shared functionality
		doGet(request, response);
	}
}