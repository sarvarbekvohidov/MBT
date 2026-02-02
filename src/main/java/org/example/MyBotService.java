package org.example;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.checkerframework.checker.units.qual.K;
import org.conscrypt.OpenSSLMac;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import javax.print.Doc;
import java.nio.channels.SelectableChannel;
import java.security.Key;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static org.example.FirebaseService.getDb;

public class MyBotService {
    public static final Map<Long, Integer> gradePage = new ConcurrentHashMap<>();

    public static Map<Long, String> teacherState = new HashMap<>();
    public static Map<Long, String> selectedClass = new HashMap<>();
    public static Map<Long, String> selectedSubject = new HashMap<>();
    public static Map<Long, String> selectedStudent = new HashMap<>();

    public static Map<Long, String> hwState = new HashMap<>();
    public static Map<Long, String> hwClass = new HashMap<>();
    public static Map<Long, String> hwSubject = new HashMap<>();

    public static Map<Long, String> notifState = new HashMap<>();
    public static Map<Long, String> notifClass = new HashMap<>();



    public static SendMessage homeworks(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        try {
            DocumentSnapshot user = FirebaseService.getUserByChatId(chatId);
            if (user == null) {
                message.setText("❌ Foydalanuvchi topilmadi");
                return message;
            }

            String role = user.getString("role");
            String classId;

            if ("student".equals(role)) {
                classId = user.getString("classId");

            } else if ("parent".equals(role)) {
                String childId = user.getString("linkedChildId");

                if (childId == null || childId.isEmpty()) {
                    message.setText("❌ ushbu ota-onaga bog‘langan bola topilmadi.");
                    return message;
                }

                DocumentSnapshot child = FirebaseService.getUser(childId);
                if (child == null || !child.exists()) {
                    message.setText("❌ bola malumotlari mavjud emas.");
                    return message;
                }

                classId = child.getString("classId");

            } else {
                message.setText("❌ bu bolim faqat ota-ona va o‘quvchilar uchun.");
                return message;
            }

            List<QueryDocumentSnapshot> homeworks = new ArrayList<>(
                    FirebaseService.getDb()
                            .collection("homeworks")
                            .whereEqualTo("classId", classId)
                            .get()
                            .get()
                            .getDocuments()
            );

            if (homeworks.isEmpty()) {
                message.setText("📭 Hozircha uyga vazifa mavjud emas.");
                return message;
            }

            homeworks.sort((a, b) ->
                    Long.compare(b.getLong("timeStamp"), a.getLong("timeStamp")));

            String text = "📚 *" + classId + "* sinf uchun uyga vazifalar:\n\n";

            for (QueryDocumentSnapshot hw : homeworks) {
                String subject = hw.getString("subject");
                String hwMessage = hw.getString("text");

                Long deadline = hw.getLong("deadline");
                String deadlineStr = new SimpleDateFormat("dd/MM/yyyy")
                        .format(new Date(deadline));

                if(deadline <= System.currentTimeMillis()){
                    deadlineStr += "(Muddat tugagan! ⌛)";
                }

                text += "📘 *" + subject + "*\n" +
                        "✏️ " + hwMessage + "\n" +
                        "⌛ Muddati: " + deadlineStr + "\n\n";
            }

            message.setParseMode("Markdown");
            message.setText(text);
            return message;

        } catch (Exception e) {
            e.printStackTrace();
            message.setText("❌ Uyga vazifa ma'lumotlarini olishda xatolik!");
            return message;
        }
    }

    public SendMessage greeting(Long chatId) {
        SendMessage message = new SendMessage();

        DocumentSnapshot user = FirebaseService.getUserByChatId(chatId.toString());
        if (user != null && user.exists()) {
            String name = user.getString("fullName");
            message.setChatId(chatId);
            message.setText("Salom " + name + "! Nima qilmoqchisiz?");
            return message;
        }

        message.setChatId(chatId);
        message.setText("⚠️ Foydalanuvchi topilmadi!");
        return message;
    }

    public SendMessage secretBack(Long chatId){
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);

       msg.setText("Congratulations! You just discovered secret message! Nothing happens!");

        return msg;
    }

    public SendMessage requestMobile(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📱 Iltimos, raqamingizni jo'nating:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);


        KeyboardButton phone = new KeyboardButton("📞 Raqamni yuborish");
        phone.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(phone);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage userDontExist(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Kechirasiz, sizda login bolmagan!");

        return message;
    }

    public SendMessage parentMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📃 Ota-onalar paneli. Iltimos, kerakli bo'limni tanlang: ");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton grades = new KeyboardButton();
        KeyboardButton homeworks = new KeyboardButton();
        grades.setText("📄 Baholarni ko'rish");
        homeworks.setText("📚 Uyga vazifalarni ko'rish");
        row1.add(grades);
        row1.add(homeworks);

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton contactTeacher = new KeyboardButton();
        contactTeacher.setText("📞 Ustozlar bilan bog'lanish");
        KeyboardButton logout = new KeyboardButton("🔙 Chiqish");
        row2.add(contactTeacher);
        row2.add(logout);

        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage studentMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        message.setText("👨‍🎓 O'quvchi menusi: ");

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton homeworks = new KeyboardButton();
        homeworks.setText("\uD83D\uDCDA Uyga vazifalar");
        row1.add(homeworks);

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton exit = new KeyboardButton();
        exit.setText("\uD83D\uDD19 Chiqish");
        row2.add(exit);

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        return message;
    }

    public SendMessage teacherMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📘 *Ustozlar paneli*\nKerakli bo'limni tanlang:");
        message.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        // row 1
        KeyboardButton btnAddGrade = new KeyboardButton();
        btnAddGrade.setText("✏️ Baholarni qo'yish");

        KeyboardButton btnAddHomework = new KeyboardButton();
        btnAddHomework.setText("📘 Vazifa berish");

        KeyboardRow row1 = new KeyboardRow();
        row1.add(btnAddGrade);
        row1.add(btnAddHomework);

        // row 2
        KeyboardButton btnNews = new KeyboardButton();
        btnNews.setText("🗣️ Yangilik qo'shish");

        KeyboardRow row2 = new KeyboardRow();
        row2.add(btnNews);

        // row 3
        KeyboardButton btnExit = new KeyboardButton();
        btnExit.setText("🔙 Chiqish");

        KeyboardRow row3 = new KeyboardRow();
        row3.add(btnExit);

        // add rows to keyboard
        List<KeyboardRow> allRows = new ArrayList<>();
        allRows.add(row1);
        allRows.add(row2);
        allRows.add(row3);

        keyboardMarkup.setKeyboard(allRows);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage grades(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        try {
            DocumentSnapshot parent = FirebaseService.getUserByChatId(chatId.toString());
            String childId = parent.getString("linkedChildId");

            // If no child
            if (childId == null || childId.isEmpty()) {
                message.setText("Bola topilmadi!");
                return message;
            }

            List<QueryDocumentSnapshot> grades = FirebaseService.getDb()
                    .collection("grades")
                    .whereEqualTo("studentPhone", childId)
                    .get()
                    .get()
                    .getDocuments();

            if (grades.isEmpty()) {
                message.setText("Hozircha baholar mavjud emas");
                return message;
            }

            Map<String, List<QueryDocumentSnapshot>> grouped = new HashMap<>();

            for (QueryDocumentSnapshot d : grades) {
                Long time = d.getLong("timeStamp");
                String date = new SimpleDateFormat("dd/MM/yyyy")
                        .format(new Date(time));

                grouped.computeIfAbsent(date, k -> new ArrayList<>()).add(d);
            }

            List<String> dates = new ArrayList<>(grouped.keySet());
            dates.sort(Collections.reverseOrder());

            int page = gradePage.getOrDefault(chatId, 0);

            if (page < 0) page = 0;
            if (page >= dates.size()) page = dates.size() - 1;
            gradePage.put(chatId, page);


            String selectedDate = dates.get(page);
            List<QueryDocumentSnapshot> todayGrades = grouped.get(selectedDate);


            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton next = new InlineKeyboardButton();
            next.setText("Next →");
            next.setCallbackData("grades_next");

            InlineKeyboardButton prev = new InlineKeyboardButton();
            prev.setText("← Previous");
            prev.setCallbackData("grades_prev");

            List<InlineKeyboardButton> row = new ArrayList<>();

            row.add(prev);
            row.add(next);

            rows.add(row);

            markup.setKeyboard(rows);

            message.setReplyMarkup(markup);


            String text = "\uD83D\uDCC5 " + selectedDate + " ----\n\n";

            for (QueryDocumentSnapshot doc : todayGrades) {
                String subject = doc.getString("subject");
                String score = doc.get("score").toString();

                text += "\uD83D\uDCDA " + subject + ": " + score + "\n";
            }

            message.setText(text);


            return message;
        } catch (Exception e) {
            e.printStackTrace();
            message.setText("❌ Baholarni korishda hatolik yuz berdi!");
            return message;
        }
    }

    public EditMessageText editGrades(Long chatId, Integer messageId) {
        SendMessage updated = grades(chatId);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setText(updated.getText());
        edit.setReplyMarkup((InlineKeyboardMarkup) updated.getReplyMarkup());

        return edit;
    }

    public SendMessage teachersPhones(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);



        try {
            DocumentSnapshot user = FirebaseService.getUserByChatId(chatId);
            if (user == null) {
                message.setText("❌ Foydalanuvchi topilmadi");
                return message;
            }

            String role = user.getString("role");
            String classId;

            if ("student".equals(role)) {
                classId = user.getString("classId");

            } else if ("parent".equals(role)) {
                String childId = user.getString("linkedChildId");

                if (childId == null || childId.isEmpty()) {
                    message.setText("❌ ushbu ota-onaga bog‘langan bola topilmadi.");
                    return message;
                }

                DocumentSnapshot child = FirebaseService.getUser(childId);
                if (child == null || !child.exists()) {
                    message.setText("❌ bola malumotlari mavjud emas.");
                    return message;
                }

                classId = child.getString("classId");

            } else {
                message.setText("❌ bu bolim faqat ota-ona va o‘quvchilar uchun.");
                return message;
            }

            List<QueryDocumentSnapshot> teachers = FirebaseService.getDb()
                    .collection("users")
                    .whereEqualTo("role", "teacher")
                    .whereArrayContains("classes", classId)
                    .get()
                    .get()
                    .getDocuments();

            if(teachers.isEmpty()){
                message.setText("Ushbu sinfga dars o'tuvchi hech qanday o'qituvchi topilmadi! ❌");
            }


            String text = "📞 *" + classId + "O'qituvchilar raqami \n\n";
            for(QueryDocumentSnapshot t : teachers){
                String name = t.getString("fullName");
                String phone = t.getString("phoneNumber");
                List<String> subjects = (List<String>) t.get("subjects");

                text += "🧑‍🏫 *" + name + "\n" + "📘 Fanlar: " + subjects + "\n" + "📱 Telefon: " + phone + "\n\n";
            }

            message.setText(text);

            return message;

        } catch (Exception e) {
            e.printStackTrace();
            message.setText("❌ Nimadir hato ketdi!");
            return message;
        }

    }

    public SendMessage userExit(String chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        FirebaseService.removeChatId(chatId);

        message.setText("⚠️ Siz tizimdan chiqdingiz!");
        return message;
    }

    public SendMessage chooseClass(Long chatId) {
        SendMessage msg = new SendMessage(chatId.toString(),
                "📘 Qaysi sinfga baho qo‘ymoqchisiz?");

        DocumentSnapshot teacher = FirebaseService.getUserByChatId(chatId.toString());
        List<String> classes = (List<String>) teacher.get("classes");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String c : classes) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("📚 " + c);
            btn.setCallbackData("grade_class_" + c);

            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        return msg;
    }

    public SendMessage chooseSubject(Long chatId) {
        SendMessage msg = new SendMessage(chatId.toString(),
                "📚 Qaysi fandan baho qo‘ymoqchisiz?");

        DocumentSnapshot teacher = FirebaseService.getUserByChatId(chatId.toString());
        List<String> subjects = (List<String>) teacher.get("subjects");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String s : subjects) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("📘 " + s);
            btn.setCallbackData("grade_subject_" + s);
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public SendMessage chooseStudent(Long chatId) {
        String classId = selectedClass.get(chatId);

        SendMessage msg = new SendMessage(chatId.toString(),
                "👤 Qaysi o‘quvchiga baho qo‘ymoqchisiz?");

        List<QueryDocumentSnapshot> students =
                null;
        try {
            students = FirebaseService.getDb()
                    .collection("users")
                    .whereEqualTo("role", "student")
                    .whereEqualTo("classId", classId)
                    .get()
                    .get()
                    .getDocuments();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (QueryDocumentSnapshot s : students) {
            String phone = s.getString("phoneNumber");
            String name = s.getString("fullName");

            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(name);
            btn.setCallbackData("grade_student_" + phone);

            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public SendMessage chooseClassForHW(Long chatId){
        SendMessage msg = new SendMessage(chatId.toString(),
                "📘 Qaysi sinfga vazifa berasiz?");

        DocumentSnapshot teacher = FirebaseService.getUserByChatId(chatId.toString());
        List<String> classes = (List<String>) teacher.get("classes");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(String c : classes){
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("📚 " + c);
            btn.setCallbackData("hw_class_" + c);
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public SendMessage chooseSubjectForHW(Long chatId){
        SendMessage msg = new SendMessage(chatId.toString(),
                "📘 Qaysi fandan vazifa berasiz?");

        DocumentSnapshot teacher = FirebaseService.getUserByChatId(chatId.toString());
        List<String> subjects = (List<String>) teacher.get("subjects");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(String s : subjects){
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("📘 " + s);
            btn.setCallbackData("hw_subject_" + s);
            rows.add(List.of(btn));
        }

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        return msg;
    }

    public SendMessage chooseNotifClass(Long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("\uD83D\uDCE2 Qaysi sinfga habar yubormoqchisiz");

        DocumentSnapshot teacher = FirebaseService.getUserByChatId(chatId.toString());
        List<String> classes = (List<String>) teacher.get("classes");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for(String c : classes){
            InlineKeyboardButton btn = new InlineKeyboardButton();
            List<InlineKeyboardButton> row = new ArrayList<>();
            btn.setText("🏫 " + c);
            btn.setCallbackData("notif_class_" + c);
           row.add(btn);
           rows.add(row);
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        return message;
    }

    public SendMessage messageSent(Long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Xabar yuborildi \uD83D\uDCE2");

        return message;
    }

    public SendMessage sendMessageNot(String chatId, String text){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔊 Yangi xabar:\n\n" + text);

        return message;
    }

}
