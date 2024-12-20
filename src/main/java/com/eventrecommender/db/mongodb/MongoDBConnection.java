package com.eventrecommender.db.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class MongoDBConnection implements DBConnection {
    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase db;

    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new MongoDBConnection();
        }
        return instance;
    }

    private MongoDBConnection() {
        // Step 1: Connect to MongoDB
        mongoClient = MongoClients.create(MongoDBUtil.MONGO_URI);
        db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
        initializeCollections();
    }

    /**
     * Initialize collections: create collections, ensure indexes, and insert
     * initial data.
     */
    private void initializeCollections() {
        // Users Collection
        MongoCollection<Document> usersCollection = db.getCollection("users");
        usersCollection.drop(); // Remove old collection
        usersCollection.insertOne(new Document()
                .append("first_name", "John")
                .append("last_name", "Smith")
                .append("password", "3229c1097c00d282d586be050")
                .append("user_id", "1111"));
        usersCollection.createIndex(new Document("user_id", 1), new IndexOptions().unique(true));
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public void setFavoriteItems(String userId, List<String> itemIds) {
        MongoCollection<Document> usersCollection = db.getCollection("users");
        usersCollection.updateOne(
                eq("user_id", userId),
                new Document("$push", new Document("favorite", new Document("$each", itemIds))));
    }

    @Override
    public void unsetFavoriteItems(String userId, List<String> itemIds) {
        MongoCollection<Document> usersCollection = db.getCollection("users");
        usersCollection.updateOne(
                eq("user_id", userId),
                new Document("$pullAll", new Document("favorite", itemIds)));
    }

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

    @Override
    public List<Item> searchItems(double lat, double lon) {
        ExternalAPI api = ExternalAPIFactory.getExternalAPI();
        List<Item> items = api.getNearbyEvents(lat, lon);
        for (Item item : items) {
            saveItem(item);
        }
        return items;
    }

    @Override
    public List<Item> searchItemsRecommended(String userId, double lat, double lon, String term) {
        System.out.println("Inside searchItemsRecommended");
        System.out.println("Params - userId: " + userId + ", lat: " + lat + ", lon: " + lon + ", term: " + term);

        ExternalAPI api = ExternalAPIFactory.getExternalAPI();
        List<Item> items = api.searchEventsByKeyword(lat, lon, term);

        if (items == null) {
            System.out.println("API returned null items for term: " + term);
        } else {
            System.out.println("Items retrieved: " + items.size());
        }

        for (Item item : items) {
            if (item != null) {
                saveItem(item);
            } else {
                System.out.println("Null item encountered while saving.");
            }
        }

        return items;
    }

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
