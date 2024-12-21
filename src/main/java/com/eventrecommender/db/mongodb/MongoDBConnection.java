package com.eventrecommender.db.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.*;
import org.bson.Document;

import com.eventrecommender.db.mysql.DBConnection;
import com.eventrecommender.entity.Item;
import com.eventrecommender.entity.Item.ItemBuilder;
import com.eventrecommender.external.ExternalAPI;
import com.eventrecommender.external.ExternalAPIFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.FindIterable;

/**
 * This class handles the MongoDB database operations and acts as an implementation of the DBConnection interface.
 */
public class MongoDBConnection implements DBConnection {
    private static MongoDBConnection instance; // Singleton instance
    private MongoClient mongoClient;          // MongoDB client
    private MongoDatabase db;                 // Reference to the database

    /**
     * Singleton pattern to get a single instance of MongoDBConnection.
     * 
     * @return The single instance of DBConnection.
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new MongoDBConnection();
        }
        return instance;
    }

    /**
     * Private constructor to initialize the MongoDB connection and collections.
     */
    private MongoDBConnection() {
        // Step 1: Connect to MongoDB using the URI and database name
        mongoClient = MongoClients.create(MongoDBUtil.MONGO_URI);
        db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);

        // Step 2: Initialize collections (users, items, etc.)
        initializeCollections();
    }

    /**
     * Initializes the necessary MongoDB collections, indexes, and inserts default data.
     */
    private void initializeCollections() {
        // Initialize "users" collection
        MongoCollection<Document> usersCollection = db.getCollection("users");

        // Drop old data (if present) and insert default user
        usersCollection.drop();
        usersCollection.insertOne(new Document()
                .append("first_name", "John")
                .append("last_name", "Smith")
                .append("password", "3229c1097c00d282d586be050") // Example hashed password
                .append("user_id", "1111"));

        // Create an index on user_id to ensure uniqueness
        usersCollection.createIndex(new Document("user_id", 1), new IndexOptions().unique(true));
    }

    /**
     * Closes the MongoDB client connection.
     */
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Adds a list of item IDs to the user's favorites.
     * 
     * @param userId  The ID of the user.
     * @param itemIds The list of item IDs to add to the user's favorites.
     */
    @Override
    public void setFavoriteItems(String userId, List<String> itemIds) {
        MongoCollection<Document> usersCollection = db.getCollection("users");
        usersCollection.updateOne(
                eq("user_id", userId),
                new Document("$push", new Document("favorite", new Document("$each", itemIds))));
    }

    /**
     * Removes a list of item IDs from the user's favorites.
     * 
     * @param userId  The ID of the user.
     * @param itemIds The list of item IDs to remove from the user's favorites.
     */
    @Override
    public void unsetFavoriteItems(String userId, List<String> itemIds) {
        MongoCollection<Document> usersCollection = db.getCollection("users");
        usersCollection.updateOne(
                eq("user_id", userId),
                new Document("$pullAll", new Document("favorite", itemIds)));
    }

    /**
     * Retrieves a set of favorite item IDs for a given user.
     * 
     * @param userId The ID of the user.
     * @return A set of favorite item IDs.
     */
    @Override
    public Set<String> getFavoriteItemIds(String userId) {
        Set<String> favoriteItems = new HashSet<>();
        MongoCollection<Document> usersCollection = db.getCollection("users");
        Document userDoc = usersCollection.find(Filters.eq("user_id", userId)).first();

        if (userDoc != null && userDoc.containsKey("favorite")) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) userDoc.get("favorite");
            if (list != null) {
                favoriteItems.addAll(list);
            }
        } else {
            System.out.println("No favorites found for userId: " + userId);
        }

        return favoriteItems;
    }

    /**
     * Retrieves a set of favorite items (details) for a given user.
     * 
     * @param userId The ID of the user.
     * @return A set of favorite items.
     */
    @Override
    public Set<Item> getFavoriteItems(String userId) {
        Set<String> itemIds = getFavoriteItemIds(userId);
        Set<Item> favoriteItems = new HashSet<>();
        MongoCollection<Document> itemsCollection = db.getCollection("items");

        for (String itemId : itemIds) {
            FindIterable<Document> iterable = itemsCollection.find(eq("item_id", itemId));
            Document doc = iterable.first();
            if (doc != null) {
                ItemBuilder builder = new ItemBuilder();
                builder.setItemId(doc.getString("item_id"));
                builder.setName(doc.getString("name"));
                builder.setCategories(getCategories(itemId));
                builder.setImageUrl(doc.getString("image_url"));
                builder.setAddress(doc.getString("address"));
                builder.setDate(doc.getString("startDate"));
                builder.setPriceRange(doc.getString("priceRange"));
                favoriteItems.add(builder.build());
            }
        }
        return favoriteItems;
    }

    /**
     * Retrieves the categories for a given item ID.
     * 
     * @param itemId The ID of the item.
     * @return A set of categories associated with the item.
     */
    @Override
    public Set<String> getCategories(String itemId) {
        Set<String> categories = new HashSet<>();
        MongoCollection<Document> itemsCollection = db.getCollection("items");
        FindIterable<Document> iterable = itemsCollection.find(eq("item_id", itemId));

        Document itemDoc = iterable.first();
        if (itemDoc != null && itemDoc.containsKey("categories")) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) itemDoc.get("categories");
            if (list != null) {
                categories.addAll(list);
            }
        }
        return categories;
    }

    /**
     * Searches for nearby items/events using latitude and longitude.
     * 
     * @param lat The latitude.
     * @param lon The longitude.
     * @return A list of nearby items/events.
     */
    @Override
    public List<Item> searchItems(double lat, double lon) {
        ExternalAPI api = ExternalAPIFactory.getExternalAPI();
        List<Item> items = api.getNearbyEvents(lat, lon);
        for (Item item : items) {
            saveItem(item);
        }
        return items;
    }

    /**
     * Searches for recommended items/events based on a search term.
     * 
     * @param userId The ID of the user.
     * @param lat    The latitude.
     * @param lon    The longitude.
     * @param term   The search term or keyword.
     * @return A list of recommended items.
     */
    @Override
    public List<Item> searchItemsRecommended(String userId, double lat, double lon, String term) {
        ExternalAPI api = ExternalAPIFactory.getExternalAPI();
        List<Item> items = api.searchEventsByKeyword(lat, lon, term);

        if (items != null) {
            for (Item item : items) {
                saveItem(item);
            }
        }

        return items;
    }

    /**
     * Saves an item to the "items" collection in MongoDB.
     * 
     * @param item The item to be saved.
     */
    @Override
    public void saveItem(Item item) {
        UpdateOptions options = new UpdateOptions().upsert(true);
        MongoCollection<Document> itemsCollection = db.getCollection("items");
        itemsCollection.updateOne(
                eq("item_id", item.getItemId()),
                new Document("$set", new Document()
                        .append("item_id", item.getItemId())
                        .append("name", item.getName())
                        .append("categories", item.getCategories())
                        .append("image_url", item.getImageUrl())
                        .append("address", item.getAddress())
                        .append("startDate", item.getDate())
                        .append("priceRange", item.getPriceRange())),
                options);
    }
}
