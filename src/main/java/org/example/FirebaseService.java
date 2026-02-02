package org.example;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.cloud.firestore.DocumentSnapshot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class FirebaseService {

    private static Firestore db;

    private static MyBot bot;

    public static void setBot(MyBot b) {
        bot = b;
    }

    public static void init() {
        try {
            String env = System.getenv("FIREBASE_KEY_JSON");
            if (env == null || env.isBlank()) {
                throw new IllegalStateException(
                        "Environment variable FIREBASE_KEY_JSON is not set or is empty"
                );
            }

            InputStream serviceAccountStream;

            // If env looks like JSON → use it directly
            if (env.trim().startsWith("{")) {
                serviceAccountStream = new ByteArrayInputStream(
                        env.getBytes(StandardCharsets.UTF_8)
                );
            }
            // Otherwise → treat it as a file path
            else {
                serviceAccountStream = new FileInputStream(env);
            }

            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(serviceAccountStream);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            db = FirestoreClient.getFirestore();
            System.out.println("✅ Firebase connected successfully");

        } catch (Exception e) {
            System.out.println("❌ Firebase initialization failed!");
            e.printStackTrace();
        }
    }



    public static Firestore getDb() {
        return db;
    }

    public static String generateId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 8; i++) {
            id.append(chars.charAt(random.nextInt(chars.length())));
        }

        return id.toString();
    }

    public static void testConnection() {
        try {
            Firestore db = getDb();
            if (db == null) {
                System.out.println("❌ Firestore instance is null. Did you call init() first?");
                return;
            }

            // Write a simple test document
            DocumentReference docRef = db.collection("testCollection").document("helloWorld");
            docRef.set(Map.of(
                    "message", "Firebase connected successfully!",
                    "timestamp", System.currentTimeMillis()
            )).get();

            System.out.println("✅ Test data written to Firestore.");

            // Read the document back
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                System.out.println("📄 Document data: " + document.getData());
            } else {
                System.out.println("⚠️ Document not found after write.");
            }

        } catch (Exception e) {
            System.out.println("❌ Firestore test failed!");
            e.printStackTrace();
        }
    }

    public static void addParent(String phoneNumber, String fullName, String linkedChildId) {
        try {
            Firestore db = getDb();

            Map<String, Object> parentData = new HashMap<>();
            parentData.put("chatId", ""); // empty for now
            parentData.put("phoneNumber", phoneNumber);
            parentData.put("fullName", fullName);
            parentData.put("role", "parent");
            parentData.put("linkedChildId", linkedChildId);
            parentData.put("timeStamp", System.currentTimeMillis());

            // use phoneNumber as document ID (easy to search later)
            ApiFuture<WriteResult> future = db.collection("users")
                    .document(phoneNumber)
                    .set(parentData);

            System.out.println("✅ Parent added successfully at: " + future.get().getUpdateTime());

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to add parent: " + phoneNumber);
        }
    }

    public static boolean userExistsByPhone(String phoneNumber) {
        try {
            Firestore db = getDb();

            // Normalize phone format (just in case Telegram sends without '+')
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            }

            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .get()
                    .getDocuments();

            return !docs.isEmpty(); // true if found

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean userExistsByChatId(String chatId) {
        try {
            Firestore db = getDb();
            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("chatId", chatId)
                    .get()
                    .get()
                    .getDocuments();

            return !docs.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static DocumentSnapshot getUser(String userId) {
        try {
            Firestore db = getDb();
            DocumentSnapshot document = db.collection("users").document(userId).get().get();

            if (document.exists()) {
                return document;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setChatIdByPhone(String phoneNumber, String chatId) {
        try {
            Firestore db = getDb();
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            }

            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("phoneNumber", phoneNumber)
                    .get()
                    .get()
                    .getDocuments();

            if (!docs.isEmpty()) {
                String docId = docs.get(0).getId();
                db.collection("users").document(docId).update("chatId", chatId);
                System.out.println("✅ ChatId is now set for number: " + phoneNumber);
            } else {
                System.out.println("⚠️ There was no user with phone number: " + phoneNumber);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DocumentSnapshot getUserByChatId(String chatId) {
        try {
            Firestore db = getDb();
            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("chatId", chatId)
                    .get()
                    .get()
                    .getDocuments();

            if (!docs.isEmpty()) {
                return docs.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUserRole(String chatId) {
        try {
            Firestore db = getDb();

            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("chatId", chatId)
                    .get()
                    .get()
                    .getDocuments();

            if (!docs.isEmpty()) {
                return docs.get(0).getString("role");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void addStudent(String phoneNumber, String fullName, String classId, String parentPhone) {
        try {
            Firestore db = getDb();

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("chatId", "");
            studentData.put("phoneNumber", phoneNumber);
            studentData.put("fullName", fullName);
            studentData.put("role", "student");
            studentData.put("classId", classId);
            studentData.put("parentPhone", parentPhone);
            studentData.put("timeStamp", System.currentTimeMillis());

            ApiFuture<WriteResult> future = db.collection("users").document(phoneNumber).set(studentData);

            System.out.println("✅ Student added successfully at: " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to add student: " + phoneNumber);
        }
    }

    public static void addGrade(String studentPhone, String teacherPhone, String subject, String score) {
        try {
            Map<String, Object> grade = new HashMap<>();
            grade.put("studentPhone", studentPhone);
            grade.put("teacherPhone", teacherPhone);
            grade.put("subject", subject);
            grade.put("score", score);
            grade.put("timeStamp", System.currentTimeMillis());

            String documentId = generateId();

            ApiFuture<WriteResult> future = db.collection("grades")
                    .document(documentId)
                    .set(grade);

            System.out.println("Grade uploaded! " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("Something went wrong while pushing new grade!");
        }
    }

    public static void addTeacher(String phoneNumber, String name, List<String> subjects, List<String> classes) {
        try {
            Firestore db = getDb();

            Map<String, Object> teacherData = new HashMap<>();

            teacherData.put("chatId", "");
            teacherData.put("phoneNumber", phoneNumber);
            teacherData.put("role", "teacher");
            teacherData.put("fullName", name);
            teacherData.put("subjects", subjects);
            teacherData.put("classes", classes);
            teacherData.put("timeStamp", System.currentTimeMillis());

            ApiFuture<WriteResult> future = db.collection("users")
                    .document(phoneNumber)
                    .set(teacherData);

            System.out.println("Teacher added succesfully at: " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("Failed to search the phoneNumber: " + phoneNumber);
        }
    }

    public static Long parseDataToMilis(String date) {
        try {
            date = date.replace("-", "/").replace(".", "/");

            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            Date d = format.parse(date);
            return d.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    public static void addHomework(String classId, String subject, String text, String deadline) {
        try {
            Firestore db = getDb();

            Map<String, Object> homeworkData = new HashMap<>();

            homeworkData.put("classId", classId);
            homeworkData.put("subject", subject);
            homeworkData.put("text", text);
            homeworkData.put("deadline", parseDataToMilis(deadline));
            homeworkData.put("timeStamp", System.currentTimeMillis());

            ApiFuture<WriteResult> future = db.collection("homeworks")
                    .document(generateId())
                    .set(homeworkData);

            System.out.println("New homework added successfully at: " + future.get().getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.out.println("Something went wrong while adding new homework!");
        }
    }

    public static void removeChatId(String chatId) {
        try {
            Firestore db = getDb();

            List<QueryDocumentSnapshot> docs = db.collection("users")
                    .whereEqualTo("chatId", chatId)
                    .get()
                    .get()
                    .getDocuments();

            if (docs.isEmpty()) {
                System.out.println("⚠️ No user found for the chatId: " + chatId);
                return;
            }

            String docId = docs.get(0).getId();

            db.collection("users")
                    .document(docId)
                    .update("chatId", "");


            System.out.println("User logged out: " + chatId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to remove chatId for user: " + chatId);
        }
    }

    public static void addNotification(String classId, String senderPhone, String text) {
        try {
            Firestore db = getDb();

            Map<String, Object> notif = new HashMap<>();
            notif.put("notifId", generateId());
            notif.put("classId", classId);
            notif.put("sender", senderPhone);
            notif.put("text", text);
            notif.put("timeStamp", System.currentTimeMillis());

            db.collection("notifications").add(notif).get();
            System.out.println("Notification saved!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Notification save failed!");
        }
    }

    public static void sendNotificationToClass(String classId, String text) {
        try {
            Firestore db = getDb();

            List<QueryDocumentSnapshot> students = db.collection("users")
                    .whereEqualTo("role", "student")
                    .whereEqualTo("classId", classId)
                    .get()
                    .get()
                    .getDocuments();

            for(QueryDocumentSnapshot s : students){
                String chatId = s.getString("chatId");

                if(chatId != null && !chatId.isEmpty()){
                    bot.sendMessageToChat(chatId, text);
                }
            }
            System.out.println("Notification sent to all students ✅");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("❌ Notification couldn't be sent");
        }
    }


}
