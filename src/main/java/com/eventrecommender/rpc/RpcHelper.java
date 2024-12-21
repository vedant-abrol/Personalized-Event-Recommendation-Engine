package com.eventrecommender.rpc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.eventrecommender.entity.Item;

/**
 * Utility class to handle JSON parsing and writing for HTTP requests and responses.
 */
public class RpcHelper {

    /**
     * Reads a `JSONObject` from the HTTP request body.
     * This method is useful for processing POST or PUT requests where the client sends JSON data.
     *
     * @param request The `HttpServletRequest` object.
     * @return A `JSONObject` parsed from the request body, or `null` if an error occurs.
     */
    public static JSONObject readJsonObject(HttpServletRequest request) {
        StringBuffer jb = new StringBuffer(); // Holds the JSON content
        String line = null;
        try {
            BufferedReader reader = request.getReader(); // Get the request body
            while ((line = reader.readLine()) != null) {
                jb.append(line); // Append each line to the buffer
            }
            reader.close(); // Close the reader
            return new JSONObject(jb.toString()); // Parse and return the JSON object
        } catch (Exception e) {
            e.printStackTrace(); // Log any exception
        }
        return null; // Return null if an error occurs
    }

    /**
     * Writes a `JSONObject` to the HTTP response.
     * Sets the content type to `application/json` and allows cross-origin requests.
     *
     * @param response The `HttpServletResponse` object.
     * @param obj      The `JSONObject` to write to the response.
     */
    public static void writeJsonObject(HttpServletResponse response, JSONObject obj) {
        try {
            response.setContentType("application/json"); // Set response content type
            response.addHeader("Access-Control-Allow-Origin", "*"); // Allow CORS
            PrintWriter out = response.getWriter(); // Get the response writer
            out.print(obj.toString()); // Write the JSON object
            out.flush(); // Flush the writer
            out.close(); // Close the writer
        } catch (Exception e) {
            e.printStackTrace(); // Log any exception
        }
    }

    /**
     * Writes a `JSONArray` to the HTTP response.
     * Sets the content type to `application/json` and allows cross-origin requests.
     *
     * @param response The `HttpServletResponse` object.
     * @param array    The `JSONArray` to write to the response.
     */
    public static void writeJsonArray(HttpServletResponse response, JSONArray array) {
        try {
            response.setContentType("application/json"); // Set response content type
            response.addHeader("Access-Control-Allow-Origin", "*"); // Allow CORS
            PrintWriter out = response.getWriter(); // Get the response writer
            out.print(array.toString()); // Write the JSON array
            out.flush(); // Flush the writer
            out.close(); // Close the writer
        } catch (Exception e) {
            e.printStackTrace(); // Log any exception
        }
    }

    /**
     * Converts a list of `Item` objects into a `JSONArray`.
     * Each `Item` object is transformed into a `JSONObject` using its `toJSONObject` method.
     *
     * @param items The list of `Item` objects.
     * @return A `JSONArray` representing the list of items.
     */
    public static JSONArray getJSONArray(List<Item> items) {
        JSONArray result = new JSONArray(); // Create an empty JSON array
        try {
            for (Item item : items) {
                result.put(item.toJSONObject()); // Convert each item to JSON and add to the array
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log any exception
        }
        return result; // Return the populated JSON array
    }
}