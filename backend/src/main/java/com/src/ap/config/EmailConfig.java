package com.src.ap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email service.
 * Maps values from application.yml under the 'email' prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "email")
@Getter
@Setter
public class EmailConfig {

    private From from = new From();
    private Branding branding = new Branding();

    @Getter
    @Setter
    public static class From {
        private String address;
        private String name;
    }

    @Getter
    @Setter
    public static class Branding {
        private String companyName;
        private String supportEmail;
        private String logoUrl;
    }
}
