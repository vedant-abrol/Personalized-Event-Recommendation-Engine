package com.eventrecommender.offline;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.eventrecommender.db.mongodb.MongoDBUtil;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;

/**
 * This class analyzes access logs stored in a MongoDB collection to identify peak traffic times.
 * It uses a 15-minute bucket interval to group and sort access counts.
 */
public class FindPeak {
    private static final String COLLECTION_NAME = "logs"; // MongoDB collection to analyze
    private static final String TIME = "time"; // Field in the collection representing the time of the request
    private static final String URL_PREFIX = "/EventRecommender"; // Filter for URLs starting with this prefix
    private static List<LocalTime> buckets = initBuckets(); // Predefined 15-minute time buckets

    public static void main(String[] args) {
        // Connect to MongoDB
        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://vedant:event@eventrecommendation.d59t7.mongodb.net/EventRecommendation")) {
            // Access the database and the logs collection
            MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            // Build the MongoDB aggregation pipeline
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", new Document("url", new Document("$regex", "^" + URL_PREFIX)))); // Match documents with URLs starting with the prefix
            pipeline.add(new Document("$group", new Document("_id", "$" + TIME)
                    .append("count", new Document("$sum", 1)))); // Group by time and count occurrences
            pipeline.add(new Document("$sort", new Document("count", -1))); // Sort groups by count in descending order

            // Run the aggregation pipeline
            AggregateIterable<Document> results = collection.aggregate(pipeline);

            // Process the aggregated results
            Map<String, Double> timeMap = new HashMap<>();
            for (Document document : results) {
                String time = findBucket(document.getString("_id")); // Group into the nearest 15-minute bucket
                Double count = document.getDouble("count"); // Access the count value
                timeMap.put(time, timeMap.getOrDefault(time, 0.0) + count); // Add to the bucket
            }

            // Sort the results by count in descending order
            List<Map.Entry<String, Double>> timeList = new ArrayList<>(timeMap.entrySet());
            Collections.sort(timeList, Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()));

            // Print the sorted results
            printList(timeList);
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions
        }
    }

    /**
     * Prints the list of time buckets and their respective access counts.
     *
     * @param timeList A list of time buckets and their counts, sorted by count.
     */
    private static void printList(List<Map.Entry<String, Double>> timeList) {
        for (Map.Entry<String, Double> entry : timeList) {
            System.out.println("Time: " + entry.getKey() + " | Count: " + entry.getValue());
        }
    }

    /**
     * Initializes 15-minute time buckets for the entire day (96 intervals).
     *
     * @return A list of LocalTime objects representing the 15-minute intervals.
     */
    private static List<LocalTime> initBuckets() {
        List<LocalTime> buckets = new ArrayList<>();
        LocalTime time = LocalTime.parse("00:00"); // Start at midnight
        for (int i = 0; i < 96; ++i) { // 96 intervals of 15 minutes each in a day
            buckets.add(time);
            time = time.plusMinutes(15);
        }
        return buckets;
    }

    /**
     * Finds the nearest 15-minute bucket for a given time.
     *
     * @param currentTime A string representing the current time in "HH:mm" format.
     * @return The nearest bucket time as a string.
     */
    private static String findBucket(String currentTime) {
        LocalTime curr = LocalTime.parse(currentTime); // Parse the input time
        int left = 0, right = buckets.size() - 1;

        // Binary search to find the nearest bucket
        while (left + 1 < right) {
            int mid = (left + right) / 2;
            if (buckets.get(mid).isAfter(curr)) {
                right = mid - 1;
            } else {
                left = mid;
            }
        }

        // Determine the closest bucket
        if (buckets.get(right).isAfter(curr)) {
            return buckets.get(left).toString();
        }
        return buckets.get(right).toString();
    }
}
