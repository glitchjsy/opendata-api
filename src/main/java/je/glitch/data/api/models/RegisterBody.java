package je.glitch.data.api.models;

import lombok.Data;

@Data
public class RegisterBody {
    private final String email;
    private final String password;
    private final String recaptchaToken;
}
