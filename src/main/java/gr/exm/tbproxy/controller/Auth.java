package gr.exm.tbproxy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.exm.tbproxy.model.User;
import gr.exm.tbproxy.model.UserLogin;
import gr.exm.tbproxy.model.UserRegister;
import gr.exm.tbproxy.service.TbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@SuppressWarnings("unused")
@RestController
public class Auth {

    @Autowired
    TbService tb;

    Logger logger = LoggerFactory.getLogger(Auth.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Log in",
            description = "Log into the system using valid credentials"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized / User not found"),
    })
    @PostMapping(value = "/auth/login")
    public JsonNode login(@RequestBody UserLogin user) {
        return tb.login(user);
    }

    @Operation(summary = "Register user",
            description = "Create a new Farmer or Agronomist"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User created"),
            @ApiResponse(responseCode = "409", description = "User already exists"),
    })
    @PostMapping(value = "/auth/register")
    public JsonNode register(@RequestBody UserRegister user) {
        try {
            return tb.register(user)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create user", null));
        } catch (HttpClientErrorException.BadRequest ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists", ex.getCause());
        }
    }

    @Operation(summary = "Refresh token",
            description = "Refresh your access token to renew your session"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refresh token succeed"),
            @ApiResponse(responseCode = "401", description = "JWT expired"),
    })
    @PostMapping(value = "/auth/refresh")
    public JsonNode refresh(@RequestBody JsonNode refreshToken) {
        return tb.refreshToken(refreshToken.get("refreshToken").asText());
    }

    @Operation(summary = "Get user",
            description = "Fetch user info"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User info"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/auth/user")
    public Optional<User> getUserInfo(@RequestHeader("X-Authorization") String fullToken) {
        String token = fullToken.split(" ")[1];
        return tb.userInfo(token);
    }

}
