package je.glitch.data.api.models;

import lombok.Data;

@Data
public class VerifyEmailBody {
    private final String token;
}
