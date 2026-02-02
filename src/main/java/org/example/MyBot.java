package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;

import static org.example.MyBotService.*;

public class MyBot extends TelegramLongPollingBot {

    MyBotService functions = new MyBotService();

    public void executeMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    public void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToChat(String chatId, String text) {
        SendMessage msg = new SendMessage(chatId, text);
        executeMessage(msg);
    }

    private String admin = "6959013020";

    @Override
    public void onUpdateReceived(Update update) {

        // for callback queries
        if(update.hasCallbackQuery()){
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer msgId = update.getCallbackQuery().getMessage().getMessageId();

            if(data.equals("grades_next")){
                int page = MyBotService.gradePage.getOrDefault(chatId, 0);
                MyBotService.gradePage.put(chatId, page + 1);
                executeMessage(functions.editGrades(chatId, msgId));
            } else if (data.equals("grades_prev")) {
                int page = MyBotService.gradePage.getOrDefault(chatId, 0);
                if (page > 0) MyBotService.gradePage.put(chatId, page - 1);
                executeMessage(functions.editGrades(chatId, msgId));
            } else if (data.startsWith("grade_class_")) {
                String classId = data.substring("grade_class_".length());

                selectedClass.put(chatId, classId);
                teacherState.put(chatId, "select_subject");

                executeMessage(functions.chooseSubject(chatId));
                return;
            } else if (data.startsWith("grade_subject_")) {
                String subject = data.substring("grade_subject_".length());

                selectedSubject.put(chatId, subject);
                teacherState.put(chatId, "select_student");

                executeMessage(functions.chooseStudent(chatId));
                return;
            } else if (data.startsWith("grade_student_")) {
                String studentPhone = data.substring("grade_student_".length());

                selectedStudent.put(chatId, studentPhone);
                teacherState.put(chatId, "enter_grade");

                executeMessage(new SendMessage(chatId.toString(),
                        "✏️ Bahoni kiriting (1–5 yoki ABS):"));
                return;
            } else if(data.startsWith("hw_class_")){
                String classId = data.substring("hw_class_".length());
                hwClass.put(chatId, classId);
                hwState.put(chatId, "select_subject_hw");

                executeMessage(functions.chooseSubjectForHW(chatId));
                return;
            }

            else if(data.startsWith("hw_subject_")){
                String subject = data.substring("hw_subject_".length());
                hwSubject.put(chatId, subject);
                hwState.put(chatId, "enter_homework_text");

                executeMessage(new SendMessage(chatId.toString(),
                        "✏️ Vazifa matnini kiriting:"));
                return;
            } else if(data.startsWith("notif_class_")){
                String classId = data.substring("notif_class_".length());
                notifClass.put(chatId, classId);
                notifState.put(chatId, "enter_notif_text");

                executeMessage(new SendMessage(chatId.toString(),
                        "📝 Yangilik matnini kiriting:"));
                return;
            }
            return;
        }


        if (!update.hasMessage()) return;
        Long chatId = update.getMessage().getChatId();

        // Contact
        if (update.getMessage().hasContact()) {
            String phone = update.getMessage().getContact().getPhoneNumber();

            if (FirebaseService.userExistsByPhone(phone)) {
                FirebaseService.setChatIdByPhone(phone, chatId.toString());
                executeMessage(functions.greeting(chatId));
                String role = FirebaseService.getUserRole(chatId.toString());

                switch (role){
                    case "parent" -> executeMessage(functions.parentMenu(chatId));
                    case "student" -> executeMessage(functions.studentMenu(chatId));
                    case "teacher" -> executeMessage(functions.teacherMenu(chatId));
                }
                return;
            } else {
                executeMessage(functions.userDontExist(chatId));
            }
            return;
        }

        // Text
        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();

          if(admin.equals(chatId.toString())){
              if(text.equals("/sync")){
                  GoogleSyncService.syncAll();
                  System.out.println("All synced!");
                  return;
              }  else if(text.equals("/syncParents")){
                  GoogleSyncService.syncParents();
                  System.out.println("Parents synced!");
                  return;
              } else if(text.equals("/syncStudents")){
                  GoogleSyncService.syncStudents();
                  System.out.println("Students synced!");
                  return;
              } else if(text.equals("/syncTeachers")){
                  GoogleSyncService.syncTeachers();
                  System.out.println("Teachers synced!");
                  return;
              }
          }
         // checking if user logged in
            boolean userExists = FirebaseService.userExistsByChatId(chatId.toString());

            if (!userExists) {
               // if no login from chatId, ask phone
                executeMessage(functions.requestMobile(chatId));
                return;
            }

            // if user logged in, find their role
            String role = FirebaseService.getUserRole(chatId.toString());

            if (role == null) {
                // if no role, then inform the user
                executeMessage(functions.userDontExist(chatId));
                return;
            }

            // give the menu according to who is using
            if (text.equals("/start")) {
                executeMessage(functions.greeting(chatId));
                switch (role) {
                    case "parent" -> executeMessage(functions.parentMenu(chatId));
                    case "teacher" -> executeMessage(functions.teacherMenu(chatId));
                    case "student" -> executeMessage(functions.studentMenu(chatId));
                    default -> executeMessage(functions.userDontExist(chatId));
                }
            } else {
                switch (role) {
                    case "parent" -> {
                        if(text.equals("📄 Baholarni ko'rish")){
                            executeMessage(functions.grades(chatId));
                        } else if(text.equals("📚 Uyga vazifalarni ko'rish")){
                            executeMessage(functions.homeworks(chatId.toString()));
                        } else if(text.equals("📞 Ustozlar bilan bog'lanish")){
                            executeMessage(functions.teachersPhones(chatId.toString()));
                        } else if(text.equals("🔙 Chiqish")){
                            executeMessage(functions.userExit(chatId.toString()));
                        } else if(text.equals("fuck you")){
                            executeMessage(functions.secretBack(chatId));
                        }
                    }

                    case "student" -> {
                        if(text.equals("\uD83D\uDCDA Uyga vazifalar")){
                            executeMessage(functions.homeworks(chatId.toString()));
                        } else if(text.equals("\uD83D\uDD19 Chiqish")){
                            executeMessage(functions.userExit(chatId.toString()));
                        } else if(text.equals("fuck you")){
                            executeMessage(functions.secretBack(chatId));
                        }
                    }

                    case "teacher" -> {
                        if (text.equals("✏️ Baholarni qo'yish")) {
                            System.out.println("Baholarni qo'yish ✏️");
                            teacherState.put(chatId, "select_class");
                            executeMessage(functions.chooseClass(chatId));
                        }
                        else if ("enter_grade".equals(teacherState.get(chatId))) {

                            String grade = text;
                            String studentPhone = selectedStudent.get(chatId);
                            String subject = selectedSubject.get(chatId);
                            String teacherPhone = FirebaseService
                                    .getUserByChatId(chatId.toString())
                                    .getString("phoneNumber");

                            FirebaseService.addGrade(studentPhone, teacherPhone, subject, grade);

                            teacherState.remove(chatId);
                            selectedClass.remove(chatId);
                            selectedSubject.remove(chatId);
                            selectedStudent.remove(chatId);

                            executeMessage(new SendMessage(chatId.toString(),
                                    "✅ Baho muvaffaqiyatli qo‘yildi!"));
                        }

                        if(text.equals("📘 Vazifa berish")){
                            System.out.println("📘 Vazifa berish");
                            hwState.put(chatId, "select_class_hw");
                            executeMessage(functions.chooseClassForHW(chatId));
                        }

                        // homework text
                        else if("enter_homework_text".equals(hwState.get(chatId))){
                            hwState.put(chatId, "enter_homework_deadline");
                            // Save it temporarily
                            selectedStudent.put(chatId, text); // using selectedStudent map just as temp storage
                            executeMessage(new SendMessage(chatId.toString(),
                                    "📅 Muddati (dd-MM-yyyy) formatda kiriting:"));
                        }

                        // deadline
                        else if("enter_homework_deadline".equals(hwState.get(chatId))){

                            try {
                                String hwText = selectedStudent.get(chatId);
                                String subject = hwSubject.get(chatId);
                                String classId = hwClass.get(chatId);

                                String deadlineStr = text;
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                                long deadlineMillis = sdf.parse(deadlineStr).getTime();

                                FirebaseService.addHomework(classId, subject, hwText, deadlineStr);

                                executeMessage(new SendMessage(chatId.toString(),
                                        "✅ Vazifa muvaffaqiyatli qo'shildi!"));

                            } catch (Exception e){
                                executeMessage(new SendMessage(chatId.toString(),
                                        "❌ Noto‘g‘ri sana formati! Masalan: 19-11-2025"));
                                return;
                            }

                            // remove everthing afterwards
                            hwState.remove(chatId);
                            hwClass.remove(chatId);
                            hwSubject.remove(chatId);
                            selectedStudent.remove(chatId);
                        }

                         else if(text.equals("🔙 Chiqish")){
                            System.out.println("🔙 Chiqish");
                             executeMessage(functions.userExit(chatId.toString()));
                        } else if(text.equals("🗣️ Yangilik qo'shish")){
                            System.out.println("🗣️ Yangilik qo'shish");
                            executeMessage(functions.chooseNotifClass(chatId));
                        } else if("enter_notif_text".equals(notifState.get(chatId))){
                             String notifText = text;
                             String classId = notifClass.get(chatId);
                             String teacherPhone = FirebaseService.getUserByChatId(chatId.toString()).getString("phoneNumber");

                             FirebaseService.addNotification(classId, teacherPhone, notifText);

                             FirebaseService.sendNotificationToClass(classId, notifText);

                             notifState.remove(chatId);
                             notifClass.remove(chatId);

                             executeMessage(functions.messageSent(chatId));
                         }
                         else if (text.equals("Hidden_Secret")){
                             executeMessage(functions.secretBack(chatId));
                        }
                    }
                }

            }
        }
    }

    @Override
    public String getBotUsername() {
        return "@MBT2bot";
    }

    @Override
    public String getBotToken() {
        return "8565939470:AAH1eqE7XSK3dyZr_pFGgH34nr8J_TbPKmc";
    }
}


// Me: Why this girl kinda has anime-like face
// Random guy: the brain cells are chasing you, but you are faster
// Me: at least im fast
// Random guy: Fast in bed? well that's obvious
// Me: Yeah ofc u would know
