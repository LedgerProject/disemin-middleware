package gr.exm.tbproxy.model;

import javax.validation.constraints.NotNull;

public class AccessTokenRefresh {

    @NotNull
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
