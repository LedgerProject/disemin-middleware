package gr.exm.tbproxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.exm.tbproxy.model.*;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TbService {
    // Auth
    JsonNode login(UserLogin user);

    Optional<JsonNode> register(UserRegister user);

    JsonNode refreshToken(String refreshToken);

    Optional<User> userInfo(String token);

    // Field
    List<EntityView> getFields(String token);

    EntityView createEntityView(String token, Field field);

    Optional<EntityView> getEntityView(String token, String id);

    Optional<EntityView> updateEntityView(String token, Field field);

    void deleteField(String token, String id);

    Optional<EntityView> attachToField(String token, String fieldId, String devId);

    Optional<Boolean> detachFromField(String token, String fieldId, String devId);

    List<EntityView> getFieldDevices(String token, String id);

    List<EntityView> getFieldForecasts(String token, String id);

    // Device
    List<Device> getAvailableDevices(String token);

    Device createDevice(DataLogger dataLogger);

    Optional<Device> updateDevice(DataLogger dataLogger);

    Optional<EntityView> updateEntityView(String token, DataLogger dataLogger);

    Map<String, List<JsonNode>> getTelemetry(String token, String id, List<String> keys, Map<String, String> params);

    Map<String, List<JsonNode>> getTelemetry(String token, String id, String key, Map<String, String> params);

    // Forecast
    List<Device> getAvailableForecasts();

    Device createForecast(Forecast forecast);

    Optional<Device> updateDevice(Forecast forecast);

    Optional<EntityView> updateEntityView(String token, Forecast forecast);

    Optional<Boolean> createTelemetry(String token, String id, String key, JsonNode data);

    Optional<List<String>> getForecastKeys(String id);

    // utils
    Field toField(EntityView ev) throws JsonProcessingException;

    DataLogger toDataLogger(EntityView ev) throws JsonProcessingException;

    DataLogger toDataLogger(Device dev) throws JsonProcessingException;

    Forecast toForecast(EntityView ev) throws JsonProcessingException;
}
