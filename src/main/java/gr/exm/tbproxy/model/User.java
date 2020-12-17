package gr.exm.tbproxy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {

    private String username;
    private String role;
    private String firstName;
    private String lastName;

    @JsonIgnore
    private AccessToken accessToken;

    public User(org.thingsboard.server.common.data.User tbUser, String role, AccessToken accessToken) {
        this.username = tbUser.getEmail();
        this.role = role;
        this.firstName = tbUser.getFirstName();
        this.lastName = tbUser.getLastName();
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

}
