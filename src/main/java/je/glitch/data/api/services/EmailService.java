package je.glitch.data.api.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class EmailService {

    private static final String APPLICATION_NAME = "Gmail API Java Email Service";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String SERVICE_ACCOUNT_FILE = "secrets/googleCloud.json";
    private static final String DELEGATED_EMAIL = "no-reply@glitch.je"; // the real mailbox to send from

    private final Gmail service;

    public EmailService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Use service account credentials with domain-wide delegation
        HttpRequestInitializer credentialsAdapter = getDelegatedCredentialsAdapter();

        this.service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentialsAdapter)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static HttpCredentialsAdapter getDelegatedCredentialsAdapter() throws IOException {
        // Load service account JSON
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(SERVICE_ACCOUNT_FILE))
                .createScoped(SCOPES)
                .createDelegated(DELEGATED_EMAIL); // <-- this is critical

        return new HttpCredentialsAdapter(credentials);
    }

    public void sendEmail(String toAddress, String subject, String bodyText) throws IOException {
        // Build RFC 2822 email
        String rawEmail = "From: " + DELEGATED_EMAIL + "\r\n" +
                "To: " + toAddress + "\r\n" +
                "Subject: " + subject + "\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                bodyText;

        // Encode to Base64URL
        String encodedEmail = Base64.getUrlEncoder()
                .encodeToString(rawEmail.getBytes(StandardCharsets.UTF_8));

        // Create Gmail Message and send
        Message message = new Message();
        message.setRaw(encodedEmail);
        service.users().messages().send("me", message).execute();
    }
}
