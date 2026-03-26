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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ee.sk.smartid.CertificateChoiceResponseValidator;
import ee.sk.smartid.CertificateValidator;
import ee.sk.smartid.CertificateValidatorImpl;
import ee.sk.smartid.DeviceLinkAuthenticationResponseValidator;
import ee.sk.smartid.FileTrustedCAStoreBuilder;
import ee.sk.smartid.NotificationAuthenticationResponseValidator;
import ee.sk.smartid.SignatureResponseValidator;
import ee.sk.smartid.SmartIdClient;
import ee.sk.smartid.TrustedCACertStore;

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

    @Value("${sid.truststore.trust-anchor-certs.filename}")
    private String sidTrustAnchorCertsFilename;

    @Value("${sid.truststore.trust-anchor-certs.password}")
    private String sigTrustAnchorCertsPassword;

    @Value("${sid.truststore.trusted-root-certs.filename}")
    private String sidTrustedRootCertsFilename;

    @Value("${sid.truststore.trusted-root-certs.password}")
    private String sidTrustedRootCertsPassword;

    @Bean
    public SmartIdClient smartIdClient() throws Exception {
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

        return client;
    }

    @Bean
    public DeviceLinkAuthenticationResponseValidator deviceLinkAuthenticationResponseValidator() {
        return DeviceLinkAuthenticationResponseValidator.defaultSetupWithCertificateValidator(certificateValidator());
    }

    @Bean
    public NotificationAuthenticationResponseValidator notificationAuthenticationResponseValidator() {
        return NotificationAuthenticationResponseValidator.defaultSetupWithCertificateValidator(certificateValidator());
    }

    @Bean
    public SignatureResponseValidator signatureResponseValidator() {
        return new SignatureResponseValidator(certificateValidator());
    }

    @Bean
    public CertificateChoiceResponseValidator certificateChoiceValidator(){
        return new CertificateChoiceResponseValidator(certificateValidator());
    }

    @Bean
    public CertificateValidator certificateValidator() {
        TrustedCACertStore trustedCACertStore = new FileTrustedCAStoreBuilder()
                .withTrustAnchorTruststorePath(sidTrustAnchorCertsFilename)
                .withTrustAnchorTruststorePassword(sigTrustAnchorCertsPassword)
                .withIntermediateCATruststorePath(sidTrustedRootCertsFilename)
                .withIntermediateCATruststorePassword(sidTrustedRootCertsPassword)
                .build();
        return new CertificateValidatorImpl(trustedCACertStore);
    }

}
