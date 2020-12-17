package gr.exm.tbproxy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.exm.tbproxy.model.DataLogger;
import gr.exm.tbproxy.model.Forecast;
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
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
public class Field {

    @Autowired
    TbService tb;

    Logger logger = LoggerFactory.getLogger(Field.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Get fields",
            description = "Get available fields"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get fields"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/fields")
    List<gr.exm.tbproxy.model.Field> getFields(@RequestHeader("X-Authorization") String fullToken) {

        String token = fullToken.split(" ")[1];
        return tb.getFields(token).stream().map(f -> {
            try {
                return new gr.exm.tbproxy.model.Field(f);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body", ex.getCause());
            }
        }).collect(Collectors.toList());

    }

    @Operation(summary = "Create field",
            description = "Create a new field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Field already exists"),
    })
    @PostMapping(value = "/field")
    gr.exm.tbproxy.model.Field createField(@RequestHeader("X-Authorization") String fullToken, @RequestBody gr.exm.tbproxy.model.Field body) {

        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.createEntityView(token, body);
            return tb.toField(ev);
        } catch (HttpClientErrorException.BadRequest ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entity already exists", ex.getCause());
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body", ex.getCause());
        }

    }

    @Operation(summary = "Update field",
            description = "Update a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PutMapping(value = "/field")
    gr.exm.tbproxy.model.Field updateField(@RequestHeader("X-Authorization") String fullToken, @RequestBody gr.exm.tbproxy.model.Field body) {

        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.updateEntityView(token, body)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
            return tb.toField(ev);
        } catch (HttpClientErrorException.BadRequest ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", ex.getCause());
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body", ex.getCause());
        }

    }

    @Operation(summary = "Get field",
            description = "Get a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get field"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping("/field/{id}")
    gr.exm.tbproxy.model.Field getField(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {

        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.getEntityView(token, id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
            return tb.toField(ev);
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
        }

    }

    @Operation(summary = "Delete field",
            description = "Delete a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @DeleteMapping("/field/{id}")
    boolean deleteField(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {

        String token = fullToken.split(" ")[1];
        tb.deleteField(token, id);
        return true;

    }

    @Operation(summary = "Create crop data",
            description = "Create crop data for a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Created"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping(value = "/field/{id}/crop")
    Boolean createCrop(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestBody JsonNode body) {

        String token = fullToken.split(" ")[1];
        return tb.createTelemetry(token, id, "crop", body)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save crop telemetry", null));

    }

    @Operation(summary = "Get field crop data",
            description = "Get field crop data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get crop data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/field/{id}/crop")
    Map<String, List<JsonNode>> getCrop(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestParam Map<String,String> params) {

        String token = fullToken.split(" ")[1];
        return tb.getTelemetry(token, id, "crop", params);

    }

    @Operation(summary = "Create log data",
            description = "Create log data for a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Created"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping(value = "/field/{id}/log")
    Boolean createLog(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestBody JsonNode body) {

        String token = fullToken.split(" ")[1];
        return tb.createTelemetry(token, id, "log", body)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save log telemetry", null));

    }

    @Operation(summary = "Get field log data",
            description = "Get field log data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get log data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/field/{id}/log")
    Map<String, List<JsonNode>> getLog(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestParam Map<String,String> params) {

        String token = fullToken.split(" ")[1];
        return tb.getTelemetry(token, id, "log", params);

    }

    @Operation(summary = "Attach to a device",
            description = "Gain access to a device's data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attached"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Field not found"),
    })
    @PostMapping(value = "/field/{fieldId}/attach/{devId}")
    Object attachToField(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String fieldId,
            @PathVariable String devId) {

        String token = fullToken.split(" ")[1];
        EntityView ev = tb.attachToField(token, fieldId, devId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
        try {
            if (ev.getType().equals("FORECAST")) {
                return tb.toForecast(ev);
            }
            return tb.toDataLogger(ev);
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bad location format", ex.getCause());
        }

    }

    @Operation(summary = "Detach from a device",
            description = "Revoke access from a device"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detached"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Field not found"),
    })
    @PostMapping(value = "/field/{fieldId}/detach/{devId}")
    Boolean detachFromField(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String fieldId,
            @PathVariable String devId) {

        String token = fullToken.split(" ")[1];
        return tb.detachFromField(token, fieldId, devId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));

    }

    @Operation(summary = "Get field's devices",
            description = "Get all devices of a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get field devices"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping("/field/{id}/devices")
    List<DataLogger> getFieldDevices(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {

        String token = fullToken.split(" ")[1];
        return tb.getFieldDevices(token, id).stream().map(f -> {
            try {
                return tb.toDataLogger(f);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
            }
        }).collect(Collectors.toList());

    }

    @Operation(summary = "Get field's forecasts",
            description = "Get all forecasts of a field"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get field forecasts"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping("/field/{id}/forecasts")
    List<Forecast> getFieldForecasts(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {

        String token = fullToken.split(" ")[1];
        return tb.getFieldForecasts(token, id).stream().map(f -> {
            try {
                return tb.toForecast(f);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
            }
        }).collect(Collectors.toList());

    }

}
