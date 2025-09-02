package com.jyl.isapi.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("camera")
public class PtzProperties {
    private String scheme;
    private String host;
    private int port;
    private String username;
    private String password;
    private int channel;

    public String getBaseUrl() {
        return String.format("%s://%s:%d", scheme, host, port);
    }
}
