package gr.exm.tbproxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.exm.tbproxy.Config;
import gr.exm.tbproxy.model.*;
import gr.exm.tbproxy.thingsboard.client.RestClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Service
public class TbRestService implements TbService {

    private final Config config;

    private final RestClient client;

    private final Logger logger = LoggerFactory.getLogger(TbRestService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String FIELD_TYPE = "FIELD";
    private final String WS_TYPE = "WEATHER STATION";
    private final String VWS_TYPE = "FORECAST";

    private final String OWNERSHIP_RELATION_TYPE = "Manages";
    private final String INCLUSION_RELATION_TYPE = "Contains";

    @Autowired
    public TbRestService(Config config) {
        this.config = config;
        this.client = new RestClient(config.getUrl());
        client.login(config.getTenant(), config.getPassword());
    }


    // Auth
    @Override
    public JsonNode login(UserLogin user) {
        return client.loginTransparently(user.getUsername(), user.getPassword());
    }

    @Override
    public Optional<JsonNode> register(UserRegister user) {
        // create a customer first
        Customer customer = new Customer();
        customer.setTitle(user.getUsername());
        Customer savedCustomer = client.saveCustomer(customer);

        ObjectNode info = objectMapper.createObjectNode();
        info.put("role", user.getRole());

        // prepare user object
        org.thingsboard.server.common.data.User newUser = new org.thingsboard.server.common.data.User();
        newUser.setEmail(user.getUsername());
        newUser.setAuthority(Authority.CUSTOMER_USER);
        newUser.setCustomerId(savedCustomer.getId());
        newUser.setFirstName(user.getFirstName());
        newUser.setLastName(user.getLastName());
        newUser.setAdditionalInfo(info);

        // register new user
        org.thingsboard.server.common.data.User tbUser = client.saveUser(newUser, false);

        // and lastly activate new user
        return client.activateUser(tbUser.getId(), user.getPassword());
    }

    @Override
    public JsonNode refreshToken(String refreshToken) {
        return client.refreshTokenTransparently(refreshToken);
    }

    @Override
    public Optional<User> userInfo(String token) {
        AccessToken accessToken = tokenFromString(token);
        return client.getUserById(UserId.fromString(accessToken.getUserId()))
                .map(user -> {
                    JsonNode info = user.getAdditionalInfo();
                    String role = info.get("role").asText();
                    return new User(user, role, accessToken);
                });
    }


    // Field
    @Override
    public List<EntityView> getFields(String token) {
        AccessToken accessToken = tokenFromString(token);
        CustomerId customerId = toCustomerId(accessToken.getCustomerId());
        PageLink pageLink = new PageLink(25);
        return client.getCustomerEntityViews(customerId, FIELD_TYPE, pageLink).getData();
    }

    @Override
    public EntityView createEntityView(String token, Field field) {
        AccessToken accessToken = tokenFromString(token);

        // store custom fields at additionalInfo
        ObjectNode obj = objectMapper.createObjectNode();
        obj.put("name", field.getName());
        obj.set("location", field.getLocation().asJsonNode());
        if (field.getPerimeter() != null) {
            obj.put("perimeter", field.getPerimeter());
        }

        // create new Device
        Device device = new Device();
        device.setType(FIELD_TYPE);
        device.setName(String.format("%s - %s", accessToken.getUserId(), field.getName()));
        device.setLabel(field.getName());
        device.setAdditionalInfo(obj);
        Device savedDevice = client.saveDevice(device);

        // create Entity View and assign to customer
        EntityView entityView = new EntityView();
        entityView.setCustomerId(toCustomerId(accessToken.getCustomerId()));
        entityView.setName(savedDevice.getName());
        entityView.setType(savedDevice.getType());
        entityView.setEntityId(savedDevice.getId());
        entityView.setAdditionalInfo(savedDevice.getAdditionalInfo());
        EntityView savedEntityView = client.saveEntityView(entityView);

        // create the appropriate relation from device to customer
        createOwnershipRelation(accessToken.getCustomerId(), savedDevice.getId());

        // store Crop info as telemetry if it exists
        if (field.getCurrentCrop() != null) {
            Crop crop = field.getCurrentCrop();
            ObjectNode telemetry = objectMapper.createObjectNode()
                    .put("name", crop.getName())
                    .put("description", crop.getDescription());
            ObjectNode data = objectMapper.createObjectNode()
                    .put("ts", crop.getTimestamp())
                    .set("values", objectMapper.createObjectNode().set("crop", telemetry));
            client.saveEntityTelemetry(savedDevice.getId(), data);
        }

        return savedEntityView;
    }

    @Override
    public Optional<EntityView> getEntityView(String token, String id) {
        AccessToken accessToken = tokenFromString(token);
        return client.getEntityViewById(toEntityViewId(id));
    }

    @Override
    public Optional<EntityView> updateEntityView(String token, Field field) {
        AccessToken accessToken = tokenFromString(token);
        return client.getEntityViewById(toEntityViewId(field.getId())).map(ev -> {

            // update additionalInfo
            ObjectNode obj = (ObjectNode) ev.getAdditionalInfo();
            obj.put("name", field.getName());
            obj.put("perimeter", field.getPerimeter());
            obj.set("location", field.getLocation().asJsonNode());
            ev.setAdditionalInfo(obj);
            EntityView savedEV = client.saveEntityView(ev);

            // update currentCrop if it exists
            if (field.getCurrentCrop() != null) {
                Crop crop = field.getCurrentCrop();

                // add new telemetry to referenced device
                client.saveEntityTelemetry(savedEV.getEntityId(), toTelemetry(crop));
            }

            return savedEV;
        });
    }

    @Override
    public void deleteField(String token, String id) {
        client.getEntityViewById(toEntityViewId(id)).map(ev -> {
            client.deleteEntityView(toEntityViewId(id));
            client.deleteDevice(toDeviceId(ev.getEntityId().getId().toString()));
            return null;
        });
    }

    @Override
    public Optional<EntityView> attachToField(String token, String fieldId, String devId) {
        AccessToken accessToken = tokenFromString(token);
        return client.getEntityViewById(toEntityViewId(fieldId))
                .flatMap(field -> client.getDeviceById(toDeviceId(devId)).map(device -> {

                    // create Entity View and assign to customer
                    EntityView entityView = new EntityView();
                    entityView.setCustomerId(toCustomerId(accessToken.getCustomerId()));
                    entityView.setName(String.format("%s - %s", accessToken.getCustomerId(), device.getName()));
                    entityView.setType(device.getType());
                    entityView.setEntityId(device.getId());
                    entityView.setAdditionalInfo(device.getAdditionalInfo());
                    EntityView savedEntityView = client.saveEntityView(entityView);

                    // create relations
                    createOwnershipRelation(accessToken.getCustomerId(), savedEntityView.getId());
                    createInclusionRelation(fieldId, savedEntityView.getId().toString());

                    return savedEntityView;
                }));
    }

    @Override
    public Optional<Boolean> detachFromField(String token, String fieldId, String devId) {
        return client.getRelation(toEntityViewId(fieldId), INCLUSION_RELATION_TYPE, RelationTypeGroup.COMMON, toEntityViewId(devId)).map(r -> {
            client.deleteRelation(toEntityViewId(fieldId), INCLUSION_RELATION_TYPE, RelationTypeGroup.COMMON, toEntityViewId(devId));
            return true;
        });
    }

    @Override
    public List<EntityView> getFieldDevices(String token, String id) {
        AccessToken accessToken = tokenFromString(token);
        return client.findByFrom(toEntityViewId(id), INCLUSION_RELATION_TYPE, RelationTypeGroup.COMMON).stream()
                .map(r -> client.getEntityViewById(toEntityViewId(r.getTo().getId().toString())).get())
                .filter(ev -> ev.getType().equals(WS_TYPE))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntityView> getFieldForecasts(String token, String id) {
        AccessToken accessToken = tokenFromString(token);
        return client.findByFrom(toEntityViewId(id), INCLUSION_RELATION_TYPE, RelationTypeGroup.COMMON).stream()
                .map(r -> client.getEntityViewById(toEntityViewId(r.getTo().getId().toString())).get())
                .filter(ev -> ev.getType().equals(VWS_TYPE))
                .collect(Collectors.toList());
    }

    // Device
    @Override
    public List<Device> getAvailableDevices(String token) {
        AccessToken accessToken = tokenFromString(token);

        PageLink pageLink = new PageLink(500);
        return client.getTenantDevices(WS_TYPE, pageLink).getData();
    }

    @Override
    public Device createDevice(DataLogger dataLogger) {
        // store custom fields at additionalInfo
        ObjectNode obj = objectMapper.createObjectNode();
        obj.put("name", dataLogger.getName());
        obj.set("location", dataLogger.getLocation().asJsonNode());

        // create new device
        Device dev = new Device();
        dev.setType(WS_TYPE);
        dev.setName(dataLogger.getName());
        dev.setLabel(dataLogger.getName());
        dev.setAdditionalInfo(obj);

        return client.saveDevice(dev);
    }

    @Override
    public Optional<Device> updateDevice(DataLogger dataLogger) {

        return client.getDeviceById(toDeviceId(dataLogger.getId())).map(dev -> {
            // udpate additionalInfo
            ObjectNode obj = (ObjectNode) dev.getAdditionalInfo();
            obj.put("name", dev.getName());
            obj.set("location", dataLogger.getLocation().asJsonNode());

            // update device
            dev.setType(WS_TYPE);
            dev.setName(dataLogger.getName());
            dev.setLabel(dataLogger.getName());
            dev.setAdditionalInfo(obj);

            return client.saveDevice(dev);
        });

    }

    @Override
    public Optional<EntityView> updateEntityView(String token, DataLogger dataLogger) {
        AccessToken accessToken = tokenFromString(token);
        return client.getEntityViewById(toEntityViewId(dataLogger.getId())).map(ev -> {
            ObjectNode obj = (ObjectNode) ev.getAdditionalInfo();
            obj.put("name", dataLogger.getName());
            obj.set("location", dataLogger.getLocation().asJsonNode());
            ev.setName(dataLogger.getName());
            ev.setAdditionalInfo(obj);
            return client.saveEntityView(ev);
        });
    }

    @Override
    public Optional<Boolean> createTelemetry(String token, String id, String key, JsonNode data) {
        AccessToken accessToken = tokenFromString(token);
        EntityViewId evId = toEntityViewId(id);
        return client.getEntityViewById(evId).map(ev -> {
            EntityId deviceId = ev.getEntityId();
            ObjectNode values = objectMapper.createObjectNode().set(key, data);
            ObjectNode telemetry = objectMapper.createObjectNode()
                    .put("ts", data.get("timestamp").longValue())
                    .set("values", values);
            return client.saveEntityTelemetry(deviceId, telemetry);
        });
    }

    @Override
    public Map<String, List<JsonNode>> getTelemetry(String token, String id, String key, Map<String, String> params) {
        Long startMs = Long.valueOf(params.getOrDefault("startTs", "0"));
        Long endMs = Long.valueOf(params.getOrDefault("endTs", "10008132529139"));
        Aggregation agg = Aggregation.valueOf(params.getOrDefault("agg", "NONE"));
        Long interval = params.containsKey("interval") ? Long.valueOf(params.get("interval")) : null;
        TimePageLink pageLink = new TimePageLink(50000, 0, null, null, startMs, endMs);
        return client.getTimeseries(toEntityViewId(id), Collections.singletonList(key), interval, agg, pageLink);
    }

    @Override
    public Map<String, List<JsonNode>> getTelemetry(String token, String id, List<String> defaultKeys, Map<String, String> params) {
        Long startMs = Long.valueOf(params.getOrDefault("startTs", "0"));
        Long endMs = Long.valueOf(params.getOrDefault("endTs", "10008132529139"));
        Aggregation agg = Aggregation.valueOf(params.getOrDefault("agg", "NONE"));
        Long interval = params.containsKey("interval") ? Long.valueOf(params.get("interval")) : null;
        List<String> keys = params.containsKey("keys") ? Arrays.asList(params.get("keys").split(",")) : defaultKeys;
        TimePageLink pageLink = new TimePageLink(50000, 0, null, null, startMs, endMs);
        return client.getTimeseries(toEntityViewId(id), keys, interval, agg, pageLink);
    }

    @Override
    public Optional<List<String>> getForecastKeys(String id) {
        return client.getEntityViewById(toEntityViewId(id)).map(ev -> {
            EntityId deviceId = ev.getEntityId();
            return client.getTimeseriesKeys(deviceId);
        });
    }

    // Forecast
    @Override
    public List<Device> getAvailableForecasts() {
        PageLink pageLink = new PageLink(25);
        return client.getTenantDevices(VWS_TYPE, pageLink).getData();
    }

    @Override
    public Device createForecast(Forecast forecast) {
        JsonNode info = objectMapper.createObjectNode()
                .set("location", forecast.getLocation().asJsonNode());
        Device device = new Device();
        device.setName(forecast.getName());
        device.setLabel(forecast.getName());
        device.setType(VWS_TYPE);
        device.setAdditionalInfo(info);
        return client.saveDevice(device);
    }

    @Override
    public Optional<Device> updateDevice(Forecast forecast) {

        return client.getDeviceById(toDeviceId(forecast.getId())).map(dev -> {
            // udpate additionalInfo
            ObjectNode obj = (ObjectNode) dev.getAdditionalInfo();
            obj.put("name", dev.getName());
            obj.set("location", forecast.getLocation().asJsonNode());

            // update device
            dev.setType(VWS_TYPE);
            dev.setName(forecast.getName());
            dev.setLabel(forecast.getName());
            dev.setAdditionalInfo(obj);

            return client.saveDevice(dev);
        });

    }

    @Override
    public Optional<EntityView> updateEntityView(String token, Forecast forecast) {
        AccessToken accessToken = tokenFromString(token);
        return client.getEntityViewById(toEntityViewId(forecast.getId())).map(ev -> {
            // update additionalInfo
            ObjectNode obj = (ObjectNode) ev.getAdditionalInfo();
            obj.put("name", forecast.getName());
            obj.set("location", forecast.getLocation().asJsonNode());
            ev.setName(forecast.getName());
            ev.setAdditionalInfo(obj);
            return client.saveEntityView(ev);
        });
    }

    // Transformations
    @Override
    public Field toField(EntityView ev) throws JsonProcessingException {
        Field field = new Field(ev);
        try {
            List<TsKvEntry> crop = client.getLatestTimeseries(ev.getId(), Collections.singletonList("crop"));
            if (crop.isEmpty()) {
                field.setCurrentCrop(null);
            } else {
                TsKvEntry kvEntry = crop.get(0);
                String value = kvEntry.getJsonValue().orElseThrow(() -> new RuntimeException("Cannot parse [currentCrop]"));
                Crop lastCrop = objectMapper.readValue(value, Crop.class);
                lastCrop.setTimestamp(kvEntry.getTs());
                field.setCurrentCrop(lastCrop);
            }
        } catch (RuntimeException ex) {
            field.setCurrentCrop(null);
        }
        return field;
    }

    @Override
    public DataLogger toDataLogger(EntityView ev) throws JsonProcessingException {
        return new DataLogger(ev);
    }

    @Override
    public DataLogger toDataLogger(Device dev) throws JsonProcessingException {
        return new DataLogger(dev);
    }

    @Override
    public Forecast toForecast(EntityView ev) throws JsonProcessingException {
        return new Forecast(ev);
    }

    // Relations
    private void createOwnershipRelation(String fromId, EntityId toId) {
        System.out.printf("Creating [Manages] relation from [%s] to [%s]%n", fromId, toId);
        EntityRelation relation = new EntityRelation();
        relation.setFrom(toCustomerId(fromId));
        relation.setTo(toId);
        relation.setType(OWNERSHIP_RELATION_TYPE);
        client.saveRelation(relation);
    }

    private void createInclusionRelation(String fromId, String toId) {
        System.out.printf("Creating [Contains] relation from [%s] to [%s]%n", fromId, toId);
        EntityRelation relation = new EntityRelation();
        relation.setFrom(toEntityViewId(fromId));
        relation.setTo(toEntityViewId(toId));
        relation.setType(INCLUSION_RELATION_TYPE);
        client.saveRelation(relation);
    }

    // Utils
    private AccessToken tokenFromString(String token) {
        String key = config.getJwtKey();
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        String userId = claims.get("userId", String.class);
        String tenantId = claims.get("tenantId", String.class);
        String customerId = claims.get("customerId", String.class);
        return new AccessToken(userId, tenantId, customerId, token);
    }

    private CustomerId toCustomerId(String customerId) {
        return new CustomerId(UUID.fromString(customerId));
    }

    private DeviceId toDeviceId(String deviceId) {
        return new DeviceId(UUID.fromString(deviceId));
    }

    private EntityViewId toEntityViewId(String entityViewId) {
        return new EntityViewId(UUID.fromString(entityViewId));
    }

    private JsonNode toTelemetry(Crop crop) {
        ObjectNode telemetry = objectMapper.createObjectNode()
                .put("name", crop.getName())
                .put("description", crop.getDescription());
        return objectMapper.createObjectNode()
                .put("ts", crop.getTimestamp())
                .<ObjectNode>set("values", objectMapper.createObjectNode().set("crop", telemetry));
    }

    @Scheduled(fixedRateString = "${tb.heartbeatMs}")
    private void heartbeat() {
        logger.warn("Sending a heartbeat request to TB server");
        PageLink pageLink = new PageLink(25);
        List<Device> devs = client.getTenantDevices(WS_TYPE, pageLink).getData();
        logger.info(String.format("There are %d devices available", devs.size()));
    }
}
