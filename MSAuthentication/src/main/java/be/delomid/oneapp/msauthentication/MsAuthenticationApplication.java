package be.delomid.oneapp.msauthentication;

import be.delomid.oneapp.msauthentication.Configration.RSAKeyRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(RSAKeyRecord.class)
@ConfigurationPropertiesScan
@EnableCaching
@SpringBootApplication
public class MsAuthenticationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAuthenticationApplication.class, args);
    }

}
