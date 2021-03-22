package gr.exm.tbproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@Configuration
@ConfigurationProperties(prefix = "tb")
@EnableScheduling
public class Config {

    @NotEmpty(message = "TB_URL cannot be empty")
    private String url;

    @NotEmpty(message = "TB_TENANT should be a valid email address")
    private String tenant;

    @NotEmpty(message = "TB_PASSWORD cannot be empty")
    private String password;

    @NotEmpty(message = "TB_JWTKEY cannot be empty")
    private String jwtKey;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJwtKey() {
        return jwtKey;
    }

    public void setJwtKey(String jwtKey) {
        this.jwtKey = jwtKey;
    }
}
