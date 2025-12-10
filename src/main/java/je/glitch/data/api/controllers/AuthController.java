package je.glitch.data.api.controllers;

import io.javalin.http.Context;
import je.glitch.data.api.models.*;
import je.glitch.data.api.services.AuthService;
import je.glitch.data.api.utils.ErrorResponse;
import je.glitch.data.api.utils.ErrorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    public void handleLogin(Context ctx) {
        Session existingSession = ctx.sessionAttribute("session");
        if (existingSession != null) {
            ctx.status(400).json(new ErrorResponse(ErrorType.INVALID_REQUEST, "Already logged in"));
            return;
        }

        LoginBody loginBody = ctx.bodyAsClass(LoginBody.class);

        try {
            Session session = service.login(loginBody);

            if (session == null) {
                ctx.status(401).json(new ErrorResponse(ErrorType.NOT_AUTHORIZED, "Invalid email or password"));
                return;
            }

            User user = service.getUserByEmail(loginBody.getEmail());
            ctx.sessionAttribute("session", session);

            ctx.status(200).json(new SessionResponse(session.getLoginTime(), user));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(new ErrorResponse(ErrorType.INVALID_REQUEST, ex.getMessage()));
        }
    }

    public void handleRegister(Context ctx) {
        RegisterBody registerBody = ctx.bodyAsClass(RegisterBody.class);

        try {
            boolean status = service.register(registerBody);

            if (!status) {
                ctx.status(401).json(new ErrorResponse(ErrorType.INVALID_REQUEST, "A user with that email address already exists"));
                return;
            }
            ctx.status(200).result();
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(new ErrorResponse(ErrorType.INVALID_REQUEST, ex.getMessage()));
        }
    }

    public void handleVerifyEmail(Context ctx) {
        VerifyEmailBody body = ctx.bodyAsClass(VerifyEmailBody.class);

        try {
            boolean status = service.verifyEmail(body);

            if (!status) {
                ctx.status(401).json(new ErrorResponse(ErrorType.INVALID_REQUEST, "Failed to verify email"));
                return;
            }
            ctx.status(200).result();
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(new ErrorResponse(ErrorType.INVALID_REQUEST, ex.getMessage()));
        }
    }
}