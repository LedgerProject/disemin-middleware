package gr.exm.tbproxy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.exm.tbproxy.model.DataLogger;
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
import org.thingsboard.server.common.data.EntityView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
public class Device {

    @Autowired
    TbService tb;

    Logger logger = LoggerFactory.getLogger(Device.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Get devices",
            description = "Get available devices"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get devices"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/devices")
    List<DataLogger> getDevices(@RequestHeader("X-Authorization") String fullToken) {
        String token = fullToken.split(" ")[1];
        return tb.getAvailableDevices(token).stream().map(d -> {
            try {
                return tb.toDataLogger(d);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
            }
        }).collect(Collectors.toList());
    }

    @Operation(summary = "Create device",
            description = "Create a new device"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get device's info"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping(value = "/device")
    org.thingsboard.server.common.data.Device createDevice(@RequestBody DataLogger body) {

        try {
            return tb.createDevice(body);
        } catch (HttpClientErrorException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entity already exists", ex.getCause());
        }

    }


    @PutMapping(value = "/device/raw")
    Optional<org.thingsboard.server.common.data.Device> updateDevice(@RequestBody DataLogger body) {

        try {
            return tb.updateDevice(body);
        } catch (HttpClientErrorException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entity already exists", ex.getCause());
        }

    }

    @Operation(summary = "Update device info",
            description = "Update a device's info"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
    })
    @PutMapping(value = "/device")
    DataLogger updateDevice(@RequestHeader("X-Authorization") String fullToken, @RequestBody DataLogger body) {

        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.updateEntityView(token, body)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
            return tb.toDataLogger(ev);
        } catch (HttpClientErrorException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", ex.getCause());
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body", ex.getCause());
        }

    }

    @Operation(summary = "Get device info",
            description = "Get a device's info"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get device info"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/device/{id}")
    DataLogger getDevice(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {

        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.getEntityView(token, id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
            return new DataLogger(ev);
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
        }

    }

    @Operation(summary = "Get device telemetry",
            description = "Get a device's data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get device data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found"),
    })
    @GetMapping(value = "/device/{id}/data")
    Map<String, List<JsonNode>> getDeviceTelemetry(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestParam Map<String, String> params) {

        String token = fullToken.split(" ")[1];
        List<String> keys = tb.getForecastKeys(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
        return tb.getTelemetry(token, id, keys, params);
    }


}
