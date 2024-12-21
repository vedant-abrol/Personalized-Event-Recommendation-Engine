package com.eventrecommender.offline;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Purify class reads a log file (e.g., catalina.out), parses each line,
 * and stores the extracted information into a MongoDB collection.
 * It helps clean and structure log data for further analysis.
 */
public class Purify {
    // Constants for log file, database name, and collection name
    private static final String FILE_NAME = "/usr/local/tomcat/logs/catalina.out";
    private static final String DB_NAME = "EventRecommendation";
    private static final String COLLECTION_NAME = "logs";

    public static void main(String[] args) {
        System.out.println("Starting Purify Program...");

        try (MongoClient mongoClient = MongoClients.create("mongodb+srv://vedant:event@eventrecommendation.d59t7.mongodb.net/EventRecommendation")) {
            System.out.println("Connected to MongoDB Atlas");

            // Access the database and collection
            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);

            // Drop the existing collection to ensure a clean start
            System.out.println("Dropping existing collection: " + COLLECTION_NAME);
            collection.drop();
            System.out.println("Collection dropped successfully!");

            // Counters for logging progress
            int totalLines = 0;
            int insertedDocuments = 0;

            // Read the log file line by line
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;

                while ((line = br.readLine()) != null) {
                    totalLines++;
                    System.out.println("Reading line " + totalLines + ": " + line);

                    // Skip empty lines
                    if (!line.trim().isEmpty()) {
                        // Parse the line into a MongoDB document
                        Document logDocument = parseLogLine(line);
                        if (logDocument != null) {
                            // Insert the document into the MongoDB collection
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

            // Summary of processing
            System.out.println("Finished processing file.");
            System.out.println("Total lines read: " + totalLines);
            System.out.println("Total documents inserted: " + insertedDocuments);
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses a single line from the log file and converts it into a MongoDB document.
     * 
     * @param line The log line to parse.
     * @return A Document object representing the parsed data, or null if the line is malformed.
     */
    private static Document parseLogLine(String line) {
        try {
            // Expected log format example:
            // 0:0:0:0:0:0:0:1 - - [20/Dec/2024:00:45:59 -0500] "GET /EventRecommender/recommendation?user_id=1111&lat=40.744384&lon=-74.059151 HTTP/1.1" 200 4586
            
            // Split the line into parts
            String[] parts = line.split(" ");
            if (parts.length < 10) {
                return null; // Line does not have enough parts
            }

            // Extract relevant fields
            String ip = parts[0]; // IP address
            String timestamp = parts[3].substring(1); // Timestamp, remove leading '['
            String method = parts[5].substring(1); // HTTP method, remove leading '"'
            String url = parts[6]; // Request URL
            String status = parts[8]; // HTTP status code
            String size = parts[9]; // Response size

            // Extract date and time from timestamp using regex
            Pattern pattern = Pattern.compile("\\[(.+?):(.+)");
            Matcher matcher = pattern.matcher(parts[3]);

            String date = "Unknown";
            String time = "Unknown";
            if (matcher.find()) {
                date = matcher.group(1); // Extract date
                time = matcher.group(2); // Extract time
            }

            // Create and return a MongoDB document with the parsed fields
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
