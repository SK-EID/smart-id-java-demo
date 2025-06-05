package ee.sk.siddemo.config;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2025 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ee.sk.smartid.AuthenticationResponseValidator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.siddemo.logging.JaxRsClientRequestLogger;

@Configuration
public class SmartIdConfig {

    @Value("${sid.truststore.trusted-server-ssl-certs.filename}")
    private String sidTrustedServerSslCertsFilename;

    @Value("${sid.truststore.trusted-server-ssl-certs.password}")
    private String sidTrustedServerSslCertsPassword;

    @Value("${sid.client.relyingPartyUuid}")
    private String sidRelyingPartyUuid;

    @Value("${sid.client.relyingPartyName}")
    private String sidRelyingPartyName;

    @Value("${sid.client.applicationProviderHost}")
    private String sidApplicationProviderHost;

    @Value("${sid.client.polling-timeout-in-seconds}")
    private Integer sidPollingTimeout;

    @Value("${sid.client.open-socket-timeout-in-seconds}")
    private Integer sidOpenSocketTimeout;

    @Value("${sid.truststore.trusted-root-certs.filename}")
    private String sidTrustedRootCertsFilename;

    @Value("${sid.truststore.trusted-root-certs.password}")
    private String sidTrustedRootCertsPassword;

    @Value("${sid.client.connection-timeout-ms}")
    private Integer connectionTimeoutMs;

    @Value("${sid.client.read-timeout-ms}")
    private Integer readTimeoutMs;

    @Bean
    public ClientConfig glassfishClientConfig() {
        var clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeoutMs);
        clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeoutMs);
        clientConfig.register(new JaxRsClientRequestLogger("Smart-ID"));
        return clientConfig;
    }

    @Bean
    public SmartIdClient smartIdClient(ClientConfig glassfishClientConfig) throws Exception {
        InputStream is = SmartIdConfig.class.getResourceAsStream(sidTrustedServerSslCertsFilename);
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, sidTrustedServerSslCertsPassword.toCharArray());

        // Client setup. Note that these values are demo environment specific.
        var client = new SmartIdClient();
        client.setRelyingPartyUUID(sidRelyingPartyUuid);
        client.setRelyingPartyName(sidRelyingPartyName);
        client.setHostUrl(sidApplicationProviderHost);
        client.setTrustStore(trustStore);
        client.setPollingSleepTimeout(TimeUnit.SECONDS, sidPollingTimeout);
        client.setSessionStatusResponseSocketOpenTime(TimeUnit.SECONDS, sidOpenSocketTimeout);

        client.setNetworkConnectionConfig(glassfishClientConfig);

        return client;
    }

    @Bean
    public AuthenticationResponseValidator authenticationResponseValidator() {
        return new AuthenticationResponseValidator(sidTrustedRootCertsFilename, sidTrustedRootCertsPassword);
    }
}
