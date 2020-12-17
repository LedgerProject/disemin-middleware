package gr.exm.tbproxy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class Forecast {

    @Autowired
    TbService tb;

    Logger logger = LoggerFactory.getLogger(Forecast.class);

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Operation(summary = "Get forecasts",
            description = "Fetch available forecasts"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get forecasts"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @GetMapping(value = "/forecasts")
    List<gr.exm.tbproxy.model.Forecast> listAvailableForecasts() {
        return tb.getAvailableForecasts().stream().map(d -> {
            try {
                return new gr.exm.tbproxy.model.Forecast(d);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body", ex.getCause());
            }
        }).collect(Collectors.toList());
    }

    @Operation(summary = "Create a forecast",
            description = "Create a forecast device"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PostMapping(value = "/forecast")
    org.thingsboard.server.common.data.Device createForecast(@RequestBody gr.exm.tbproxy.model.Forecast forecast) {
        return tb.createForecast(forecast);
    }

    @PutMapping(value = "/forecast/raw")
    Optional<org.thingsboard.server.common.data.Device> updateDevice(@RequestBody gr.exm.tbproxy.model.Forecast body) {

        try {
            return tb.updateDevice(body);
        } catch (HttpClientErrorException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entity already exists", ex.getCause());
        }

    }

    @Operation(summary = "Update forecast",
            description = "Update a forecast's info"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
    })
    @PutMapping(value = "/forecast")
    Optional<gr.exm.tbproxy.model.Forecast> updateForecast(
            @RequestHeader("X-Authorization") String fullToken,
            @RequestBody gr.exm.tbproxy.model.Forecast forecast) {

        String token = fullToken.split(" ")[1];
        return tb.updateEntityView(token, forecast).map(ev -> {
            try {
                return tb.toForecast(ev);
            } catch (JsonProcessingException ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Malformed JSON body", ex.getCause());
            }
        });

    }

    @Operation(summary = "Get forecast info",
            description = "Fetch a forecast's info"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Forecast not found"),
    })
    @GetMapping("/forecast/{id}")
    gr.exm.tbproxy.model.Forecast getForecast(@RequestHeader("X-Authorization") String fullToken, @PathVariable String id) {
        try {
            String token = fullToken.split(" ")[1];
            EntityView ev = tb.getEntityView(token, id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
            return tb.toForecast(ev);
        } catch (JsonProcessingException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", ex.getCause());
        }
    }

    @Operation(summary = "Get forecast data",
            description = "Get forecast data from a specific device"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get telemetry data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not found"),
    })
    @GetMapping(value = "/forecast/{id}/data")
    Map<String, List<JsonNode>> getForecastData(
            @RequestHeader("X-Authorization") String fullToken,
            @PathVariable String id,
            @RequestParam Map<String, String> params) {

        String token = fullToken.split(" ")[1];
        List<String> keys = tb.getForecastKeys(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity does not exist", null));
        return tb.getTelemetry(token, id, keys, params);

    }

}
