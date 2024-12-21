package com.eventrecommender.offline;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Purify {
    private static final String FILE_NAME = "/usr/local/tomcat/logs/catalina.out";
    private static final String DB_NAME = "EventRecommendation";
    private static final String COLLECTION_NAME = "logs";

    public static void main(String[] args) {
        System.out.println("Starting Purify Program...");

        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://vedant:event@eventrecommendation.d59t7.mongodb.net/EventRecommendation")) {
            System.out.println("Connected to MongoDB Atlas");

            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            System.out.println("Dropping existing collection: " + COLLECTION_NAME);
            collection.drop();
            System.out.println("Collection dropped successfully!");

            int totalLines = 0;
            int insertedDocuments = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;

                while ((line = br.readLine()) != null) {
                    totalLines++;
                    System.out.println("Reading line " + totalLines + ": " + line);

                    if (!line.trim().isEmpty()) {
                        Document logDocument = parseLogLine(line);
                        if (logDocument != null) {
                            collection.insertOne(logDocument);
                            insertedDocuments++;
                            System.out.println("Inserted document: " + logDocument.toJson());
                        } else {
                            System.out.println("Skipping malformed line: " + line);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading or processing file: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("Finished processing file.");
            System.out.println("Total lines read: " + totalLines);
            System.out.println("Total documents inserted: " + insertedDocuments);
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Document parseLogLine(String line) {
        try {
            // Example log format:
            // 0:0:0:0:0:0:0:1 - - [20/Dec/2024:00:45:59 -0500] "GET /EventRecommender/recommendation?user_id=1111&lat=40.744384&lon=-74.059151 HTTP/1.1" 200 4586
            String[] parts = line.split(" ");
            if (parts.length < 10) {
                return null; // Line does not have expected parts
            }

            String ip = parts[0];
            String timestamp = parts[3].substring(1); // Removing leading [
            String method = parts[5].substring(1); // Removing leading "
            String url = parts[6];
            String status = parts[8];
            String size = parts[9];

            Pattern pattern = Pattern.compile("\\[(.+?):(.+)");
            Matcher matcher = pattern.matcher(parts[3]);

            String date = "Unknown";
            String time = "Unknown";
            if (matcher.find()) {
                date = matcher.group(1);
                time = matcher.group(2);
            }

            return new Document("ip", ip)
                    .append("date", date)
                    .append("time", time)
                    .append("method", method)
                    .append("url", url)
                    .append("status", status)
                    .append("size", size);
        } catch (Exception e) {
            System.err.println("Error parsing line: " + e.getMessage());
            return null;
        }
    }
}
