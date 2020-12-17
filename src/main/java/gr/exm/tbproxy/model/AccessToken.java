package gr.exm.tbproxy.model;

public class AccessToken {

    private String userId;
    private String tenantId;
    private String customerId;
    private String token;

    public AccessToken(String userId, String tenantId, String customerId, String token) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
