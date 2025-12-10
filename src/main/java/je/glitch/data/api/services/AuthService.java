package je.glitch.data.api.services;

import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.*;
import je.glitch.data.api.utils.ErrorResponse;
import je.glitch.data.api.utils.ErrorType;
import je.glitch.data.api.utils.HttpException;
import je.glitch.data.api.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class AuthService {
    private final MySQLConnection connection;
    private final EmailService emailService;

    public Session login(LoginBody loginBody) throws IllegalArgumentException {
        if (loginBody.getEmail() == null || loginBody.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        Optional<User> userOptional = connection.getUserTable().getUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(loginBody.getEmail()))
                .findFirst();

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        if (!BCrypt.checkpw(loginBody.getPassword(), user.getPassword())) {
            return null;
        }
        if (!user.isEmailVerified()) {
            throw new HttpException(ErrorType.FORBIDDEN, 403, "Please verify your email");
        }
        return new Session(new Date(), user.getId());
    }

    public boolean register(RegisterBody registerBody) throws IllegalArgumentException {
        if (registerBody.getEmail() == null || registerBody.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }
        if (registerBody.getRecaptchaToken() == null) {
            throw new IllegalArgumentException("Recaptcha token is missing");
        }

        if (!Utils.verifyRecaptchaToken(registerBody.getRecaptchaToken())) {
            throw new HttpException(ErrorType.INVALID_REQUEST, 400, "Recaptcha verification failed");
        }

        boolean userExists = connection.getUserTable().checkUserExists(registerBody.getEmail());

        if (userExists) {
            return false;
        }

        String id = UUID.randomUUID().toString();
        String hashedPassword = BCrypt.hashpw(registerBody.getPassword(), BCrypt.gensalt(8));
        String verificationToken = UUID.randomUUID().toString();

        User user = new User(
                id,
                null,
                null,
                registerBody.getEmail(),
                hashedPassword,
                null,
                verificationToken,
                false,
                false
        );

        boolean success = connection.getUserTable().createUser(user);

        if (success) {
            try {
                sendVerificationEmail(user);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    connection.getUserTable().deleteUser(id);
                } catch (Exception ex) {

                }
                throw new HttpException(ErrorType.SERVER_ERROR, 500, "Failed to register");
            }
            return true;
        } else {
            try {
                connection.getUserTable().deleteUser(id);
            } catch (Exception ex) {

            }
            throw new HttpException(ErrorType.SERVER_ERROR, 500, "Failed to register");
        }
    }

    public boolean verifyEmail(VerifyEmailBody body) throws IllegalArgumentException {
        if (body.getToken() == null) {
            throw new IllegalArgumentException("Missing token");
        }

        User user = connection.getUserTable().getUserByEmailVerificationToken(body.getToken());
        if (user == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        connection.getUserTable().setEmailVerified(user);
        return true;
    }

    public User getUserByEmail(String email) {
        return connection.getUserTable().getUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    private void sendVerificationEmail(User user) throws IOException {
        // You would normally store this token in DB for verification later
        String verificationLink = "https://opendata.je/verify-email?token=" + user.getEmailVerificationToken();

        String subject = "Verify your email";
        String body = "Hello,\n\nPlease verify your email by clicking the link below:\n" + verificationLink + "\n\nThank you!";

        emailService.sendEmail(user.getEmail(), subject, body);
    }
}