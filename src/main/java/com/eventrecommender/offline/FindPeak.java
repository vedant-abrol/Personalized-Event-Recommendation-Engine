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


public class FindPeak {
    private static final String COLLECTION_NAME = "logs";
    private static final String TIME = "time";
    private static final String URL_PREFIX = "/EventRecommender";
    private static List<LocalTime> buckets = initBuckets();

    public static void main(String[] args) {
        // Connect to MongoDB
        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://vedant:event@eventrecommendation.d59t7.mongodb.net/EventRecommendation")) {
            MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            // Aggregation Pipeline: Match -> Group -> Sort
            List<Document> pipeline = new ArrayList<>();
            pipeline.add(new Document("$match", new Document("url", new Document("$regex", "^" + URL_PREFIX))));
            pipeline.add(new Document("$group", new Document("_id", "$" + TIME)
                    .append("count", new Document("$sum", 1))));
            pipeline.add(new Document("$sort", new Document("count", -1))); // Sort in descending order

            // Run aggregation
            AggregateIterable<Document> results = collection.aggregate(pipeline);

            // Process results
            Map<String, Double> timeMap = new HashMap<>();
            for (Document document : results) {
                String time = findBucket(document.getString("_id")); // Grouping into 15-minute buckets
                Double count = document.getDouble("count");
                timeMap.put(time, timeMap.getOrDefault(time, 0.0) + count);
            }

            // Sort the results
            List<Map.Entry<String, Double>> timeList = new ArrayList<>(timeMap.entrySet());
            Collections.sort(timeList, Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()));

            printList(timeList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printList(List<Map.Entry<String, Double>> timeList) {
        for (Map.Entry<String, Double> entry : timeList) {
            System.out.println("Time: " + entry.getKey() + " | Count: " + entry.getValue());
        }
    }

    private static List<LocalTime> initBuckets() {
        List<LocalTime> buckets = new ArrayList<>();
        LocalTime time = LocalTime.parse("00:00");
        for (int i = 0; i < 96; ++i) { // Divide the day into 15-minute intervals
            buckets.add(time);
            time = time.plusMinutes(15);
        }
        return buckets;
    }

    private static String findBucket(String currentTime) {
        LocalTime curr = LocalTime.parse(currentTime);
        int left = 0, right = buckets.size() - 1;
        while (left + 1 < right) {
            int mid = (left + right) / 2;
            if (buckets.get(mid).isAfter(curr)) {
                right = mid - 1;
            } else {
                left = mid;
            }
        }
        if (buckets.get(right).isAfter(curr)) {
            return buckets.get(left).toString();
        }
        return buckets.get(right).toString();
    }
}
