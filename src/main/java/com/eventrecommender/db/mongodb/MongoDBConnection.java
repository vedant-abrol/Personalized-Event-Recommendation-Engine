package com.eventrecommender.db.mongodb;

import static com.mongodb.client.model.Filters.eq;

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
        // Connects to local MongoDB server using latest MongoDB driver
        String connectionString = "mongodb://localhost:27017"; // Update if needed
        mongoClient = MongoClients.create(connectionString);
        db = mongoClient.getDatabase(MongoDBUtil.DB_NAME);
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
        FindIterable<Document> iterable = usersCollection.find(eq("user_id", userId));

        Document userDoc = iterable.first();
        if (userDoc != null && userDoc.containsKey("favorite")) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) userDoc.get("favorite");
            favoriteItems.addAll(list);
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
                builder.setCity(doc.getString("city"));
                builder.setState(doc.getString("state"));
                builder.setCountry(doc.getString("country"));
                builder.setZipcode(doc.getString("zip_code"));
                builder.setRating(doc.getDouble("rating"));
                builder.setAddress(doc.getString("address"));
                builder.setLatitude(doc.getDouble("latitude"));
                builder.setLongitude(doc.getDouble("longitude"));
                builder.setDescription(doc.getString("description"));
                builder.setSnippet(doc.getString("snippet"));
                builder.setSnippetUrl(doc.getString("snippet_url"));
                builder.setImageUrl(doc.getString("image_url"));
                builder.setUrl(doc.getString("url"));
                builder.setCategories(getCategories(itemId));
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
            categories.addAll(list);
        }
        return categories;
    }

    @Override
    public List<Item> searchItems(String userId, double lat, double lon, String term) {
        // Connect to external API
        ExternalAPI api = ExternalAPIFactory.getExternalAPI();
        List<Item> items = api.search(lat, lon, term);
        for (Item item : items) {
            // Save the item into our own database
            saveItem(item);
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
                        .append("city", item.getCity())
                        .append("state", item.getState())
                        .append("country", item.getCountry())
                        .append("zip_code", item.getZipcode())
                        .append("rating", item.getRating())
                        .append("address", item.getAddress())
                        .append("latitude", item.getLatitude())
                        .append("longitude", item.getLongitude())
                        .append("description", item.getDescription())
                        .append("snippet", item.getSnippet())
                        .append("snippet_url", item.getSnippetUrl())
                        .append("image_url", item.getImageUrl())
                        .append("url", item.getUrl())
                        .append("categories", item.getCategories())
                        .append("startDate", item.getDate())
                        .append("priceRange", item.getPriceRange())),
                options);
    }
}
