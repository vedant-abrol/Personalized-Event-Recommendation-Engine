package com.eventrecommender.db.mongodb;

import java.text.ParseException;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

// Create tables for MongoDB (all pipelines).
public class MongoDBTableCreation {
    // Run as Java application to create MongoDB tables with index.
    public static void main(String[] args) throws ParseException {
        // Step 1: Create a connection to MongoDB using the latest MongoDB driver
        String connectionString = "mongodb://localhost:27017"; // Update with your MongoDB URI if needed
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            // Get the database
            MongoDatabase db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);

            // Step 2: Remove old collections (tables)
            db.getCollection("users").drop();
            db.getCollection("items").drop();

            // Step 3: Create new collections, populate data, and create indexes
            MongoCollection<Document> usersCollection = db.getCollection("users");
            usersCollection.insertOne(new Document()
                    .append("first_name", "John")
                    .append("last_name", "Smith")
                    .append("password", "3229c1097c00d282d586be050")
                    .append("user_id", "1111"));

            // Ensure user_id is unique
            IndexOptions indexOptions = new IndexOptions().unique(true);
            usersCollection.createIndex(new Document("user_id", 1), indexOptions);

            // Create items collection and ensure item_id is unique
            MongoCollection<Document> itemsCollection = db.getCollection("items");
            itemsCollection.createIndex(new Document("item_id", 1), indexOptions);

            System.out.println("Tables created successfully with indexes.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred while creating tables in MongoDB.");
        }
    }
}
