package civicpulse;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** MongoDB persistence for CivicPulse. Database: CivicPulse; collections: accounts and complaints. */
final class MongoStore {
    static MongoClient client;
    static MongoDatabase db;
    static MongoCollection<Document> accounts;
    static MongoCollection<Document> complaints;
    static volatile boolean ready = false;

    private MongoStore() { }

    /** Uses the Atlas URI from CIVICPULSE_MONGODB_URI. MONGO_URI is also accepted. */
    static boolean connect() {
        String uri = System.getenv("CIVICPULSE_MONGODB_URI");
        if (uri == null || uri.isBlank()) uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank()) {
            System.out.println("MongoDB URI not set (CIVICPULSE_MONGODB_URI). Local file storage will be used.");
            ready = false;
            return false;
        }

        try {
            client = MongoClients.create(uri);
            db = client.getDatabase("CivicPulse");
            db.runCommand(new Document("ping", 1));
            accounts = db.getCollection("accounts");
            complaints = db.getCollection("complaints");
            ready = true;
            System.out.println("MongoDB Atlas connected successfully.");
            System.out.println("Database: CivicPulse; collections: accounts, complaints");
            return true;
        } catch (Exception e) {
            ready = false;
            System.out.println("MongoDB connection failed: " + e.getMessage());
            if (client != null) {
                try { client.close(); } catch (Exception ignored) { }
                client = null;
            }
            return false;
        }
    }

    static void loadUsers(Map<String, Main.User> users) {
        for (Document d : accounts.find()) {
            String username = str(d, "username");
            if (username.isBlank()) username = str(d, "_id");
            if (username.isBlank()) continue;
            users.put(username, new Main.User(username, str(d, "passwordHash"), str(d, "role"),
                    str(d, "department"), str(d, "phone"), str(d, "name")));
        }
    }

    static void saveUsers(Collection<Main.User> list) {
        for (Main.User u : list) {
            Document d = new Document("username", u.username)
                    .append("passwordHash", u.passwordHash)
                    .append("role", u.role)
                    .append("department", u.department)
                    .append("phone", u.phone)
                    .append("name", u.name);
            accounts.replaceOne(Filters.eq("username", u.username), d, new ReplaceOptions().upsert(true));
        }
    }

    static void loadComplaints(Map<String, Main.Complaint> target) {
        for (Document d : complaints.find().sort(new Document("arrivalOrder", 1))) {
            String id = str(d, "id");
            if (id.isBlank()) id = str(d, "_id");
            if (id.isBlank()) continue;
            int arrival = num(d, "arrivalOrder", num(d, "arrival", 0));
            Main.Complaint c = new Main.Complaint(id, str(d, "citizenUsername"), str(d, "citizenName"),
                    str(d, "phone"), str(d, "department"), str(d, "location"),
                    num(d, "severity", 1), num(d, "affectedPeople", 1), str(d, "date"), str(d, "time"),
                    str(d, "description"), arrival, str(d, "status"), str(d, "assignedUsername"), str(d, "assignedName"));
            Object history = d.get("history");
            if (history instanceof List<?>) {
                for (Object item : (List<?>) history) if (item != null) c.history.add(String.valueOf(item));
            }
            target.put(c.id, c);
            Main.nextArrival = Math.max(Main.nextArrival, c.arrivalOrder + 1);
        }
    }

    static void saveComplaints(Collection<Main.Complaint> list) {
        for (Main.Complaint c : list) {
            Document d = new Document("id", c.id)
                    .append("citizenUsername", c.citizenUsername)
                    .append("citizenName", c.citizenName)
                    .append("phone", c.phone)
                    .append("department", c.department)
                    .append("location", c.location)
                    .append("severity", c.severity)
                    .append("affectedPeople", c.affectedPeople)
                    .append("date", c.date)
                    .append("time", c.time)
                    .append("description", c.description)
                    .append("arrivalOrder", c.arrivalOrder)
                    .append("status", c.status)
                    .append("assignedUsername", c.assignedUsername)
                    .append("assignedName", c.assignedName)
                    .append("history", new ArrayList<>(c.history));
            complaints.replaceOne(Filters.eq("id", c.id), d, new ReplaceOptions().upsert(true));
        }
    }

    private static String str(Document d, String key) {
        Object value = d.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int num(Document d, String key, int fallback) {
        Object value = d.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (Exception ignored) { return fallback; }
    }
}
