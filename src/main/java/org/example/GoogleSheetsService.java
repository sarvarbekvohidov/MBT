package org.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsService {

    private static Sheets sheetsService;
    public static final String SPREADSHEET_ID = "1q1dZUmOFx_S8x4wfqak2sPTDzJQ00p3TUGg4_W08bes";

    public static void init() {
        try {
            String env = System.getenv("SHEETS_KEY_JSON");
            if (env == null || env.isBlank()) {
                throw new IllegalStateException(
                        "Environment variable SHEETS_KEY_JSON is not set or is empty"
                );
            }

            InputStream serviceAccountStream;

            // If env contains raw JSON
            if (env.trim().startsWith("{")) {
                serviceAccountStream = new ByteArrayInputStream(
                        env.getBytes(StandardCharsets.UTF_8)
                );
            }
            // If env is a file path
            else {
                serviceAccountStream = new FileInputStream(env);
            }

            ServiceAccountCredentials credentials =
                    (ServiceAccountCredentials) ServiceAccountCredentials
                            .fromStream(serviceAccountStream)
                            .createScoped(List.of(SheetsScopes.SPREADSHEETS));

            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("MBT-SchoolBot")
                    .build();

            System.out.println("✅ Google Sheets connected!");

        } catch (Exception e) {
            System.out.println("❌ Google Sheets initialization failed!");
            e.printStackTrace();
        }
    }



    public static Sheets getSheetsService() {
        return sheetsService;
    }

    public static List<List<Object>> readSheet(String range) {
        try {
            Sheets sheets = getSheetsService();

            Sheets.Spreadsheets.Values.Get request =
                    sheets.spreadsheets().values().get(SPREADSHEET_ID, range);

            return request.execute().getValues();

        } catch (Exception e) {
            System.out.println("Couldn't read from google sheets");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
