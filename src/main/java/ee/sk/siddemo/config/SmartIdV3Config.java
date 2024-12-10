package ee.sk.siddemo.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ee.sk.smartid.v3.AuthenticationResponseValidator;
import ee.sk.smartid.v3.SmartIdClient;

@Configuration
public class SmartIdV3Config {

    @Value("${sid.v3.truststore.trusted-server-ssl-certs.filename}")
    private String sidTrustedServerSslCertsFilename;

    @Value("${sid.v3.truststore.trusted-server-ssl-certs.password}")
    private String sidTrustedServerSslCertsPassword;

    @Value("${sid.v3.client.relyingPartyUuid}")
    private String sidRelyingPartyUuid;

    @Value("${sid.v3.client.relyingPartyName}")
    private String sidRelyingPartyName;

    @Value("${sid.v3.client.applicationProviderHost}")
    private String sidApplicationProviderHost;

    @Value("${sid.v3.client.polling-timeout-in-seconds}")
    private Integer sidPollingTimeout;

    @Value("${sid.v3.truststore.trusted-root-certs.filename}")
    private String sidTrustedRootCertsFilename;

    @Value("${sid.v3.truststore.trusted-root-certs.password}")
    private String sidTrustedRootCertsPassword;


    @Bean
    public SmartIdClient smartIdClientV3() throws Exception {
        InputStream is = SmartIdV3Config.class.getResourceAsStream(sidTrustedServerSslCertsFilename);
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, sidTrustedServerSslCertsPassword.toCharArray());

        // Client setup. Note that these values are demo environment specific.
        var client = new SmartIdClient();
        client.setRelyingPartyUUID(sidRelyingPartyUuid);
        client.setRelyingPartyName(sidRelyingPartyName);
        client.setHostUrl(sidApplicationProviderHost);
        client.setTrustStore(trustStore);
        client.setPollingSleepTimeout(TimeUnit.SECONDS, sidPollingTimeout);

        return client;
    }

    @Bean
    public AuthenticationResponseValidator authenticationResponseValidatorV3() {
        return new AuthenticationResponseValidator(sidTrustedRootCertsFilename, sidTrustedRootCertsPassword);
    }
}
