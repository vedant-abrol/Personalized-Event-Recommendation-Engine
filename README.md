# 🎟️ **Personalized Event Recommendation Engine**

---

## **📚 Project Overview**

The **Personalized Event Recommendation Engine** is an interactive web application that allows users to discover events tailored to their preferences. Using the **TicketMaster API** for event data and advanced algorithms for personalized recommendations, the system delivers a seamless user experience for finding and managing events. 

The project is designed with a **3-tier architecture**, including a robust backend, responsive frontend, and data management systems. Real-time user behavior analysis is supported through the **ELK Stack (Elasticsearch, Logstash, Kibana)**.

---

## **🎯 Features**

- **Event Discovery:** Users can find nearby events based on their geographical location.
- **Favorites Management:** Easily add and remove favorite events with a heart toggle.
- **Personalized Recommendations:** Suggests events based on the user's favorite categories and ranks them by proximity.
- **Dark Mode:** Toggle between light and dark themes for enhanced user experience.
- **Real-Time Analysis:** Monitor system logs and user behavior with **Elasticsearch**, **Logstash**, and **Kibana**.

---

## **💡 Business Design**

### **Why?**
Many events people like are either hard to discover or irrelevant to their preferences. This system fills that gap by offering:
- Tailored event recommendations.
- A personalized user experience.

### **Use Cases**
1. **Search Nearby Events**: Locate events based on geolocation.
2. **Favorite Events**: Save events for easy access later.
3. **Get Recommendations**: Receive personalized event suggestions.

---

## **🔧 Tech Stack**

### **Frontend**
- **HTML5, CSS3**: For responsive and accessible design.
- **JavaScript (ES6)**: Core scripting for interaction.
- **Font Awesome**: Icons for visual enhancements.

### **Backend**
- **Java Servlets (Jakarta EE)**: Handles business logic.
- **MongoDB**: Stores user preferences and event data.
- **MySQL**: Stores detailed event and user information.

### **Real-Time Log Analysis**
- **Elasticsearch**: Stores and indexes logs for querying.
- **Logstash**: Processes logs and sends them to Elasticsearch.
- **Kibana**: Visualizes logs with dashboards.

---

## **📐 Architecture Overview**

### **3-Tier Architecture**
1. **Presentation Tier**: User interface using HTML, CSS, and JavaScript.
2. **Logic Tier**: Java-based business logic for search and recommendations.
3. **Data Tier**: Integration with MongoDB and MySQL for storage.

![3-Tier Architecture](https://user-images.githubusercontent.com/38120488/38473675-087633c6-3b62-11e8-8901-96afffa2c78f.png)

---

## **💻 API Overview**

### **Core APIs**
1. **Search Events**
   - Endpoint: `GET /EventRecommender/search`
   - Parameters: `user_id`, `lat`, `lon`
   - Response: List of events near the user's location.

2. **Favorites Management**
   - Add Favorite: `POST /EventRecommender/history`
   - Remove Favorite: `DELETE /EventRecommender/history`
   - Get Favorites: `GET /EventRecommender/history`

3. **Recommendations**
   - Endpoint: `GET /EventRecommender/recommendation`
   - Parameters: `user_id`, `lat`, `lon`
   - Response: Personalized event suggestions.

![API Flow](https://user-images.githubusercontent.com/38120488/38473945-be2e55a6-3b65-11e8-8358-011f267195da.png)

---

## **📊 Database Design**

### **MySQL**
- **Item Table**: Stores event details.
- **User Table**: Stores user details.
- **Category Table**: Links events and categories.
- **History Table**: Tracks user favorites.

![MySQL Design](https://user-images.githubusercontent.com/38120488/38480030-08dbcca2-3b91-11e8-8c90-184f7e818758.png)

### **MongoDB**
- **users Collection**: Stores user information and favorites.
- **items Collection**: Stores event data.
- **logs Collection**: Tracks user behavior for analysis.

---

## **🔍 Implementation Details**

### **Recommendation Algorithm**
1. **Content-Based Filtering**:
   - Analyzes user favorites.
   - Fetches similar events using **TicketMaster API**.

2. **GeoHash for Location Encoding**:
   - Converts latitude/longitude to GeoHash for API requests.

3. **Log Analysis**:
   - Analyzes traffic to identify peak times and user activity trends.

### **Handling CORS**
- Configured server to handle **Cross-Origin Resource Sharing (CORS)**:
  ```java
  response.setContentType("application/json");
  response.addHeader("Access-Control-Allow-Origin", "*");
  ```

### **Integration with ELK**
- **Logstash**: Processes logs in real-time.
- **Elasticsearch**: Stores logs for analysis.
- **Kibana**: Provides dashboards for visual insights.

![ELK Flow](https://user-images.githubusercontent.com/38120488/38480242-651a17f2-3b92-11e8-9658-8da3b5a69fb2.png)

---

## **📈 Log Analysis**

### **Behavior Analysis**
- **Kibana Dashboard**: Visualizes user activity across the system.

![Kibana Dashboard](https://user-images.githubusercontent.com/38120488/38480048-2f351ebc-3b91-11e8-9bc7-d0cf30effe3b.png)

### **Offline Analysis**
- **Peak Time Identification**:
  - Use MapReduce jobs in MongoDB.
  - Tools: [Purify.java](./src/offline/Purify.java) and [FindPeak.java](./src/offline/FindPeak.java).

---

## **🚀 Setup Instructions**

### **Prerequisites**
- Java 11+
- Apache Tomcat
- MongoDB
- ELK Stack (optional for log analysis)

### **Steps**
1. **Clone the Repository**
   ```bash
   git clone https://github.com/vedant-abrol/Personalized-Event-Recommendation-Engine.git
   cd Personalized-Event-Recommendation-Engine
   ```

2. **Set Up MongoDB**
   - Use `MongoDBConnection.java` to initialize collections.

3. **Configure TicketMaster API**
   - Update `TicketMasterAPI.java` with your API key.

4. **Deploy WAR File**
   ```bash
   sudo cp event-recommender.war /usr/local/tomcat/webapps/
   sudo systemctl restart tomcat
   ```

5. **Run Logstash Pipeline**
   ```bash
   logstash -f logstash_pipeline.conf
   ```

---

## **📸 Screenshots**

### **Home Page**
![Home Page](screenshots/Screenshot4.png)

### **Recommendations**
![Recommendations](screenshots/Screenshot3.png)

### **Favorites**
![Favorites](screenshots/Screenshot5.png)

---

### **Demo Video**
![Demo](screenshots/ScreenRecordingDemo.gif)

---

## **🔮 Future Improvements**
1. **AI-Driven Recommendations**:
   - Implement ML models for collaborative filtering.
2. **Enhanced UI/UX**:
   - Add OAuth for login.
   - Notifications for saved events.
3. **Real-Time Updates**:
   - Use WebSockets for live updates.

---

### **Thank you for using the Personalized Event Recommendation Engine! 🎟️**