package cm.odigital.serviceconnectmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import cm.odigital.serviceconnectmarket.auth.config.PasswordResetProperties;
import cm.odigital.serviceconnectmarket.auth.config.RegistrationProperties;
import cm.odigital.serviceconnectmarket.schema.SchemaVerificationProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    RegistrationProperties.class,
    PasswordResetProperties.class,
    SchemaVerificationProperties.class
})
public class ServiceConnectmarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceConnectmarketApplication.class, args);
    }

}
