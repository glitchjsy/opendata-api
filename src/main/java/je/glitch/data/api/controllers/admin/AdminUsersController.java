package je.glitch.data.api.controllers.admin;

import io.javalin.http.Context;
import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.ApiResponse;
import je.glitch.data.api.models.User;
import je.glitch.data.api.utils.ErrorResponse;
import je.glitch.data.api.utils.ErrorType;
import je.glitch.data.api.utils.HttpException;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AdminUsersController {
    private final MySQLConnection connection;

    public void handleGetUsers(Context ctx) {
        List<User> users = connection.getUserTable().getUsers();
        ctx.json(new ApiResponse<>(users));
    }

    public void handleCreateUser(Context ctx) {
        User bodyUser = ctx.bodyAsClass(User.class);
        String id = UUID.randomUUID().toString();
        String hashedPassword = BCrypt.hashpw(bodyUser.getPassword(), BCrypt.gensalt(8));

        boolean userExists = connection.getUserTable().checkUserExists(bodyUser.getEmail());

        if (userExists) {
            ctx.status(409).json(new ErrorResponse(ErrorType.ALREADY_EXISTS, "A user with that email already exists"));
            return;
        }

        User user = new User(
                id,
                null,
                null,
                bodyUser.getEmail(),
                hashedPassword,
                null,
                null,
                false,
                bodyUser.isSiteAdmin()
        );

        boolean success = connection.getUserTable().createUser(user);

        if (success) {
            User newUser = connection.getUserTable().getUser(user.getId());
            ctx.status(200).json(new ApiResponse<>(newUser));
        } else {
            throw new HttpException(ErrorType.SERVER_ERROR, 500, "Failed to create user");
        }
    }

    public void handleDeleteUser(Context ctx) {
        String userId = ctx.pathParam("userId");
        boolean success = connection.getUserTable().deleteUser(userId);

        if (success) {
            ctx.status(200).result();
        } else {
            throw new HttpException(ErrorType.NOT_FOUND, 404, "User not found");
        }
    }

    public void handleUpdateUser(Context ctx) {
        String userId = ctx.pathParam("userId");
        User bodyUser = ctx.bodyAsClass(User.class);
        User user = new User(
                userId,
                null,
                null,
                bodyUser.getEmail(),
                null,
                null,
                null,
                false,
                bodyUser.isSiteAdmin()
        );

        boolean success = connection.getUserTable().updateUser(user);

        if (success) {
            User updatedUser = connection.getUserTable().getUser(user.getId());
            ctx.status(200).json(new ApiResponse<>(updatedUser));
        } else {
            throw new HttpException(ErrorType.SERVER_ERROR, 500, "User not found or could not be updated");
        }
    }
}
