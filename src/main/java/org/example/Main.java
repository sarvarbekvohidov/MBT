package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.List;



    public class Main {
        public static void main(String[] args) {
            try {
                FirebaseService.init();
//                FirebaseService.testConnection();

                GoogleSheetsService.init();

                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

                MyBot bot = new MyBot();
                FirebaseService.setBot(bot);

                GoogleSyncService.syncAll();

                botsApi.registerBot(bot);

                System.out.println(System.getenv("FIREBASE_KEY_JSON"));

                System.out.println("Bot + health server started ✅");

            } catch (Exception e){
                e.printStackTrace();
            }
        }}