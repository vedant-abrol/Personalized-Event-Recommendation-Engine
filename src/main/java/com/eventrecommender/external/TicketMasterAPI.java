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

public class TicketMasterAPI implements ExternalAPI {
	private static final String API_HOST = "app.ticketmaster.com";
	private static final String SEARCH_PATH = "/discovery/v2/events.json";
	private static final String RECOMMEND_PATH = "/discovery/v2/suggest.json";
	private static final String API_KEY = "vAiO3Wm87BQCgSAcmtDnprIEZudbFFD5"; // use your api key

	/**
	 * Common method to handle HTTP request to TicketMaster API and return events.
	 */
	private List<Item> sendRequestToTicketMaster(String url, String query) {
		try {
			// Open an HTTP connection between your Java application and TicketMaster API
			HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + query).openConnection();
			connection.setRequestMethod("GET");

			// Log request and response status
			System.out.println("\nSending 'GET' request to URL : " + url + "?" + query);
			int responseCode = connection.getResponseCode();
			System.out.println("Response Code : " + responseCode);

			// Read the response body
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Extract events array from response
			JSONObject responseJson = new JSONObject(response.toString());
			JSONObject embedded = (JSONObject) responseJson.get("_embedded");
			JSONArray events = (JSONArray) embedded.get("events");

			return getItemList(events);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<Item> getNearbyEvents(double lat, double lon) {
		// Create a base URL for search without a keyword
		String url = "http://" + API_HOST + SEARCH_PATH;

		// Convert geo location to geo hash with a precision of 4 (+- 20km)
		String geoHash = GeoHash.encodeGeohash(lat, lon, 4);

		// Formulate the query with only geo location
		String query = String.format("apikey=%s&geoPoint=%s&radius=50", API_KEY, geoHash);
		System.out.println("Sending request for nearby events.");

		// Send request and return item list
		return sendRequestToTicketMaster(url, query);
	}

	@Override
	public List<Item> searchEventsByKeyword(double lat, double lon, String term) {
		// Create a base URL for search with a term
		String url = "http://" + API_HOST + RECOMMEND_PATH;

		// Convert geo location to geo hash with a precision of 4 (+- 20km)
		String geoHash = GeoHash.encodeGeohash(lat, lon, 4);

		// Encode term in URL to handle special characters
		term = urlEncodeHelper(term);

		// Formulate the query with keyword and geo location
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=50", API_KEY, geoHash, term);
		System.out.println("Sending request with keyword: " + term);

		// Send request and return item list
		return sendRequestToTicketMaster(url, query);
	}

	private String urlEncodeHelper(String url) {
		try {
			// java 程序需要你自己手动去转字符，browser会自动转
			url = java.net.URLEncoder.encode(url, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return url;
	}

	/**
	 * Helper methods
	 */
	// Convert JSONArray to a list of item objects.
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
			JSONObject venue = getVenue(event);
			if (venue != null) {
				if (!venue.isNull("address")) {
					JSONObject address = venue.getJSONObject("address");
					StringBuilder sb = new StringBuilder();
					if (!address.isNull("line1")) {
						sb.append(address.getString("line1"));
					}
					if (!address.isNull("line2")) {
						sb.append(address.getString("line2"));
					}
					if (!address.isNull("line3")) {
						sb.append(address.getString("line3"));
					}
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

			// Uses this builder pattern we can freely add fields.
			Item item = builder.build();
			itemList.add(item);
		}

		return itemList;
	}

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
		return null;
	}

	private String getImageUrl(JSONObject event) throws JSONException {
		// Get from “image” field
		if (!event.isNull("images")) {
			JSONArray imagesArray = event.getJSONArray("images");
			if (imagesArray.length() >= 1) {
				return getStringFieldOrNull(imagesArray.getJSONObject(0), "url");
			}
		}
		return null;
	}

	private String getDescription(JSONObject event) throws JSONException {
		if (!event.isNull("description")) {
			return event.getString("description");
		} else if (!event.isNull("additionalInfo")) {
			return event.getString("additionalInfo");
		} else if (!event.isNull("info")) {
			return event.getString("info");
		} else if (!event.isNull("pleaseNote")) {
			return event.getString("pleaseNote");
		}
		return null;
	}

	private Set<String> getCategories(JSONObject event) throws JSONException {
		// Get from “classifications” => “segment” => “name”
		Set<String> categories = new HashSet<>();
		JSONArray classifications = (JSONArray) event.get("classifications");
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

	private String getStartDate(JSONObject event) throws JSONException {
		JSONObject dates = (JSONObject) event.get("dates");
		JSONObject start = (JSONObject) dates.get("start");
		return start.getString("localDate");
	}

	private String getPriceRange(JSONObject event) throws JSONException {
		if (event.isNull("priceRanges")) {
			return "NA";
		}
		JSONObject priceRanges = ((JSONArray) event.get("priceRanges")).getJSONObject(0);
		int min = priceRanges.getInt("min");
		int max = priceRanges.getInt("max");
		return "$" + min + "-" + max;
	}

	private String getStringFieldOrNull(JSONObject event, String field) throws JSONException {
		return event.isNull(field) ? null : event.getString(field);
	}

	private double getNumericFieldOrNull(JSONObject event, String field) throws JSONException {
		return event.isNull(field) ? 0.0 : event.getDouble(field);
	}

}