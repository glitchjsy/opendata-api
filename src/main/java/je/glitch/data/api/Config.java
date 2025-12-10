package je.glitch.data.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Config {
    private static Properties config = new Properties();

    static {
        try {
            Path runPath = Paths.get("").toAbsolutePath();
            Path secretsPath = runPath.resolve("secrets/config.properties");

            FileInputStream fis = new FileInputStream(secretsPath.toFile());
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getMysqlDatabase() {
        return config.getProperty("mysql.database");
    }

    public static String getMysqlPassword() {
        return config.getProperty("mysql.password");
    }

    public static String getRecaptchaSecret() {
        return config.getProperty("recaptcha.secret");
    }

    public static void main(String[] args) {
        System.out.println("MySQL Password: " + getMysqlPassword());
        System.out.println("reCAPTCHA Token: " + getRecaptchaSecret());
    }
}
