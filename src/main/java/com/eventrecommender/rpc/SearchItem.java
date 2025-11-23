package com.eventrecommender.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

                Double lat = parseCoordinate(request.getParameter("lat"));
                Double lon = parseCoordinate(request.getParameter("lon"));

                if (lat == null || lon == null) {
                        RpcHelper.writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid latitude or longitude supplied.");
                        return;
                }

                DBConnection conn = DBConnectionFactory.getDBConnection();
                if (conn == null) {
                        LOGGER.severe("DBConnection is null. Check database connection configuration.");
                        RpcHelper.writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Service unavailable: database connection not configured.");
                        return;
                }

                try {
                        List<Item> items = conn.searchItems(lat, lon);
                        Set<String> favorite = conn.getFavoriteItemIds(userId);

                        List<JSONObject> list = new ArrayList<>();
                        for (Item item : items) {
                                JSONObject obj = item.toJSONObject();
                                if (favorite != null) {
                                        obj.put("favorite", favorite.contains(item.getItemId()));
                                }
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
