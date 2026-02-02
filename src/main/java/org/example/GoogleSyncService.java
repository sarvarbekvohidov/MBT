package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleSyncService {

    public static void syncParents() {
        System.out.println("syncing parents...");

        List<List<Object>> rows = GoogleSheetsService.readSheet("Parents!A2:C");

        for (List<Object> row : rows) {
            if (row == null) continue;

            String phone = safeGet(row, 0);
            String fullName = safeGet(row, 1);
            String linkedChild = safeGet(row, 2);

            if (phone.isBlank()) {
                System.out.println("Skipping parent row with empty phone: " + row);
                continue;
            }

            if (!FirebaseService.userExistsByPhone(phone)) {
                FirebaseService.addParent(phone, fullName, linkedChild);
                System.out.println("Parent added: " + phone);
            } else {
                System.out.println("Parent already exists: " + phone);
            }
        }
    }

    public static void syncStudents() {
        System.out.println("syncing students...");

        List<List<Object>> rows = GoogleSheetsService.readSheet("Students!A2:D");

        for (List<Object> row : rows) {
            if (row == null) continue;

            String phone = safeGet(row, 0);
            String fullName = safeGet(row, 1);
            String classId = safeGet(row, 2);
            String parentPhone = safeGet(row, 3);

            if (phone.isBlank()) {
                System.out.println("Skipping student row with empty phone: " + row);
                continue;
            }

            if (!FirebaseService.userExistsByPhone(phone)) {
                FirebaseService.addStudent(phone, fullName, classId, parentPhone);
                System.out.println("Student added: " + phone);
            } else {
                System.out.println("Student already exists: " + phone);
            }
        }
    }

    public static void syncTeachers() {
        System.out.println("syncing teachers...");

        List<List<Object>> rows = GoogleSheetsService.readSheet("Teachers!A2:D");

        for (List<Object> row : rows) {
            if (row == null) continue;

            String phone = safeGet(row, 0);
            String fullName = safeGet(row, 1);

            String subjectsRaw = safeGet(row, 2);
            List<String> subjects = parseCommaList(subjectsRaw);

            String classesRaw = safeGet(row, 3);
            List<String> classes = parseCommaList(classesRaw);

            if (phone.isBlank()) {
                System.out.println("Skipping teacher row with empty phone: " + row);
                continue;
            }

            if (!FirebaseService.userExistsByPhone(phone)) {
                FirebaseService.addTeacher(phone, fullName, subjects, classes);
                System.out.println("Teacher added: " + phone);
            } else {
                System.out.println("Teacher already exists: " + phone);
            }
        }
    }

    public static void syncAll() {
        syncParents();
        syncStudents();
        syncTeachers();
        System.out.println("sync completed!");
    }

    // helpers
    private static String safeGet(List<Object> row, int idx) {
        if (row.size() <= idx) return "";
        Object o = row.get(idx);
        if (o == null) return "";
        return o.toString().trim();
    }

    private static List<String> parseCommaList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
