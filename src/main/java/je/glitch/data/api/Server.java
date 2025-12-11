package je.glitch.data.api;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import je.glitch.data.api.cache.RedisCache;
import je.glitch.data.api.controllers.AuthController;
import je.glitch.data.api.controllers.MeController;
import je.glitch.data.api.controllers.admin.AdminStatsController;
import je.glitch.data.api.controllers.admin.AdminTokensController;
import je.glitch.data.api.controllers.admin.AdminUsersController;
import je.glitch.data.api.controllers.v1.*;
import je.glitch.data.api.database.MySQLConnection;
import je.glitch.data.api.models.Session;
import je.glitch.data.api.models.User;
import je.glitch.data.api.services.*;
import je.glitch.data.api.utils.ErrorType;
import je.glitch.data.api.utils.HttpException;
import lombok.Getter;
import org.eclipse.jetty.server.session.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static Server INSTANCE;

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .serializeNulls()
            // Temporary hack
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getName().equals("password");
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    @Getter
    private final MySQLConnection connection;

    private final CarparkController carparkController;
    private final VehicleController vehicleController;
    private final SimpleEndpointController simpleEndpointController;
    private final BusController busController;
    private final ErrorController errorController;
    private final FoiController foiController;
    private final CourtController courtController;
    private final PetitionController petitionController;

    private final AdminUsersController adminUsersController;
    private final AdminTokensController adminTokensController;
    private final AdminStatsController adminStatsController;

    private final AuthController authController;
    private final MeController meController;

    private final EmailService emailService;

    private final RedisCache cache;

    private final ExecutorService trackingThreadPool = Executors.newFixedThreadPool(4);

    public Server() {
        INSTANCE = this;

        this.connection = new MySQLConnection();
        this.cache = new RedisCache();

        this.carparkController = new CarparkController(new CarparkService(connection, cache));
        this.vehicleController = new VehicleController(new VehicleService(connection));
        this.busController = new BusController(new BusService(connection));
        this.simpleEndpointController = new SimpleEndpointController(connection, cache);
        this.errorController = new ErrorController();
        this.foiController = new FoiController(new FoiService(connection));
        this.courtController = new CourtController(new CourtService(connection));
        this.petitionController = new PetitionController(connection);

        try {
            this.emailService = new EmailService();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.authController = new AuthController(new AuthService(connection, emailService));
        this.meController = new MeController(new ApiKeyService(connection), new UserService(connection));
        this.adminUsersController = new AdminUsersController(connection);
        this.adminTokensController = new AdminTokensController(connection);
        this.adminStatsController = new AdminStatsController(connection);
    }

    public static void main(String[] args) {
        new Server().startup();
    }

    private void startup() {
        JsonMapper gsonMapper = new JsonMapper() {
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return GSON.toJson(obj, type);
            }

            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return GSON.fromJson(json, targetType);
            }
        };

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(gsonMapper);
            config.router.ignoreTrailingSlashes = true;
            config.showJavalinBanner = false;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.path = "/v1/*";
                    it.anyHost();
                    it.allowCredentials = false;
                });
                cors.addRule(it -> {
                    it.path = "/*";
                    it.allowHost(
                            "http://localhost:3000",
                            "http://127.0.0.1:3000",
                            "https://data.glitch.je",
                            "https://opendata.je"
                    );
                    it.allowCredentials = true;
                });
            });
            config.jetty.modifyServletContextHandler(handler -> handler.setSessionHandler(sqlSessionHandler()));
        }).start(8080);

        app.exception(Exception.class, errorController::handleException);
        app.error(404, errorController::handleNotFound);

        // Admin handling
        app.before(ctx -> {
            String path = ctx.path();

            // Let CORS plugin handle it
            if (ctx.method().name().equalsIgnoreCase("OPTIONS")) {
                return;
            }

            if (path.startsWith("/admin")) {
                Session session = ctx.sessionAttribute("session");

                if (session != null) {
                    User user = connection.getUserTable().getUser(session.getUserId());

                    if (user != null && user.isSiteAdmin()) {
                        return;
                    }
                }
                throw new HttpException(ErrorType.FORBIDDEN, 403, "You must be an administrator to access this endpoint");
            }
        });

        // Enforce user agent header
        app.before(ctx -> {
            String path = ctx.path();
            String userAgent = ctx.header("user-agent");

            if (path.startsWith("/v1") && userAgent == null) {
                throw new HttpException(ErrorType.MISSING_USER_AGENT, 400, ErrorType.MISSING_USER_AGENT.getDefaultMessage());
            }
        });

        // Analytics tracking
        app.after(ctx -> {
            String path = ctx.path();

            // We only care about public API endpoints
            if (!path.startsWith("/v1")) {
                return;
            }

            String method = ctx.method().name();
            int status = ctx.statusCode();
            String ip = ctx.ip();
            String userAgent = ctx.userAgent();
            String tokenHeader = ctx.header("x-api-key");
            String tokenQuery = ctx.queryParam("auth");

            trackingThreadPool.submit(() -> {
                String apiTokenId = null;
                String token = null;

                if (tokenHeader != null && !tokenHeader.isEmpty()) {
                    token = tokenHeader;
                } else if (tokenQuery != null && !tokenQuery.isEmpty()) {
                    token = tokenQuery;
                }
                if (token != null) {
                    String foundId = connection.getApiKeyTable().getIdFromKey(token);
                    apiTokenId = (foundId != null && !foundId.isEmpty()) ? foundId : null;
                }
                connection.getLogTable().trackRequest(method, path, status, ip, userAgent, apiTokenId);
            });
        });

        app.head("/health", ctx -> ctx.status(200));
        app.head("/health/fetcher", simpleEndpointController::handleGetFetcherHeartbeat);

        app.get("/v1/carparks", carparkController::handleGetCarparks);
        app.get("/v1/carparks/spaces", carparkController::handleGetLiveSpaces);
        app.get("/v1/carparks/spaces/dates", carparkController::handleGetLiveSpacesDates);
        app.get("/v1/carparks/spaces/dates/{date}", carparkController::handleGetLiveSpacesForDate);
        app.get("/v1/carparks/spaces/all-temp-do-not-use", carparkController::handleGetAllSpacesData);
        app.get("/v1/carparks/{idOrCode}", carparkController::handleGetCarpark);

        app.get("/v1/vehicles", vehicleController::handleGetVehicles);
        app.get("/v1/vehicles/stats", vehicleController::handleGetStats);
        app.get("/v1/vehicles/colors", vehicleController::handleGetColors);
        app.get("/v1/vehicles/makes", vehicleController::handleGetMakes);
        app.get("/v1/vehicles/models", vehicleController::handleGetModels);
        app.get("/v1/vehicles/lookup/{plate}", vehicleController::handleGetPlate);

        app.get("/v1/eatsafe", simpleEndpointController::handleGetEatsafe);
        app.get("/v1/toilets", simpleEndpointController::handleGetToilets);
        app.get("/v1/recycling", simpleEndpointController::handleGetRecycling);
        app.get("/v1/defibrillators", simpleEndpointController::handleGetDefibrillators);
        app.get("/v1/foi-requests", foiController::handleGetFoiRequests);
        app.get("/v1/foi-requests/stats", foiController::handleGetStats);
        app.get("/v1/foi-requests/authors", foiController::handleGetAuthors);
        app.get("/v1/foi-requests/producers", foiController::handleGetProducers);
        app.get("/v1/foi-requests/{id}", foiController::handleGetById);

        app.get("/v1/courts/magistrates/hearings", courtController::handleGetMagistratesHearings);
        app.get("/v1/courts/magistrates/results", courtController::handleGetMagistratesResults);
        app.get("/v1/courts/magistrates/hearings/fields", courtController::handleGetDistinctMagistratesHearingFields);
        app.get("/v1/courts/magistrates/results/fields", courtController::handleGetDistinctMagistratesResultFields);

        app.get("/v1/charts/parking-stats", carparkController::handleGetParkingStats);
        app.get("/v1/charts/bus-passengers", simpleEndpointController::handleGetBusPassengersChart);
        app.get("/v1/charts/road-traffic", simpleEndpointController::handleGetRoadTrafficChart);
        app.get("/v1/charts/driving-test-results", simpleEndpointController::handleGetDrivingResultsChart);
        app.get("/v1/charts/monthly-rainfall", simpleEndpointController::handleGetMonthlyRainfallChart);
        app.get("/v1/charts/registered-vehicles", simpleEndpointController::handleGetRegisteredVehiclesChart);
        app.get("/v1/charts/petition-stats", petitionController::handleGetPetitionStats);

        app.get("/v1/bus/routes", busController::handleGetRoutes);
        app.get("/v1/bus/stops", busController::handleGetStops);
        app.get("/v1/bus/stops/{id}", busController::handleGetStop);
        app.get("/v1/bus/live-updates", busController::handleGetLiveUpdates);
        app.get("/v1/bus/live-updates-minimal", busController::handleGetMinUpdates);

        app.get("/v1/petitions", petitionController::handleGetPetitions);

        app.get("/admin/users", adminUsersController::handleGetUsers);
        app.post("/admin/users/new", adminUsersController::handleCreateUser);
        app.post("/admin/users/{userId}", adminUsersController::handleUpdateUser);
        app.delete("/admin/users/{userId}", adminUsersController::handleDeleteUser);
        app.get("/admin/users/{userId}/tokens", adminTokensController::handleListTokensForUser);
        app.get("/admin/api-keys", adminTokensController::handleListTokens);
        app.post("/admin/api-keys/new", adminTokensController::handleCreateToken);
        app.delete("/admin/api-keys/{tokenId}", adminTokensController::handleDeleteToken);
        app.get("/admin/stats", adminStatsController::handleGetStats);
        app.get("/admin/stats/daily-requests", adminStatsController::handleGetDailyRequestsChart);
        app.get("/admin/stats/top-endpoints", adminStatsController::handleGetTopEndpoints);

        app.post("/auth/login", authController::handleLogin);
        app.post("/auth/register", authController::handleRegister);
        app.post("/auth/verify-email", authController::handleVerifyEmail);
        app.get("/auth/logout", authController::handleLogout);

        app.get("/me/session", meController::handleGetSession);
        app.get("/me/api-keys", meController::handleListApiKeys);
        app.delete("/me/api-keys/{tokenId}", meController::handleDeleteApiKey);
        app.post("/me/api-keys/new", meController::handleCreateApiKey);
    }

    private SessionHandler sqlSessionHandler() {
        SessionHandler sessionHandler = new SessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
        sessionCache.setSessionDataStore(
                jdbcDataStoreFactory().getSessionDataStore(sessionHandler)
        );
        sessionHandler.setSessionCache(sessionCache);
        sessionHandler.setHttpOnly(true);
        return sessionHandler;
    }

    private JDBCSessionDataStoreFactory jdbcDataStoreFactory() {
        DatabaseAdaptor databaseAdaptor = new DatabaseAdaptor();
        databaseAdaptor.setDriverInfo("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost/opendata");
        databaseAdaptor.setDatasource(connection.getDataSource());
        JDBCSessionDataStoreFactory jdbcSessionDataStoreFactory = new JDBCSessionDataStoreFactory();
        jdbcSessionDataStoreFactory.setDatabaseAdaptor(databaseAdaptor);
        return jdbcSessionDataStoreFactory;
    }
}
