package ee.sk.siddemo;

/*-
 * #%L
 * Smart-ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import ee.sk.siddemo.model.UserSidSession;
import ee.sk.smartid.AuthenticationResponseValidator;
import ee.sk.smartid.SmartIdClient;

@Configuration
public class Config {

    @Value("${sid.client.relyingPartyUuid}")
    private String sidRelyingPartyUuid;

    @Value("${sid.client.relyingPartyName}")
    private String sidRelyingPartyName;

    @Value("${sid.client.applicationProviderHost}")
    private String sidApplicationProviderHost;

    @Value("${sid.truststore.trusted-server-ssl-certs.filename}")
    private String sidTrustedServerSslCertsFilename;

    @Value("${sid.truststore.trusted-server-ssl-certs.password}")
    private String sidTrustedServerSslCertsPassword;

    @Value("${sid.truststore.trusted-root-certs.filename}")
    private String sidTrustedRootCertsFilename;

    @Value("${sid.truststore.trusted-root-certs.password}")
    private String sidTrustedRootCertsPassword;

    @Bean
    public SmartIdClient smartIdClient() throws Exception {
        InputStream is = Config.class.getResourceAsStream(sidTrustedServerSslCertsFilename);
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, sidTrustedServerSslCertsPassword.toCharArray());

        // Client setup. Note that these values are demo environment specific.
        var client = new SmartIdClient();
        client.setRelyingPartyUUID(sidRelyingPartyUuid);
        client.setRelyingPartyName(sidRelyingPartyName);
        client.setHostUrl(sidApplicationProviderHost);
        client.setTrustStore(trustStore);

        return client;
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION,
            proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserSidSession userSessionSigning() {
        return new UserSidSession();
    }

    @Bean
    public AuthenticationResponseValidator sidResponseValidator() throws Exception {

        List<X509Certificate> certificates = new ArrayList<>();

        InputStream is = Config.class.getResourceAsStream(sidTrustedRootCertsFilename);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, sidTrustedRootCertsPassword.toCharArray());
        Enumeration<String> aliases = keystore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            X509Certificate certificate = (X509Certificate) keystore.getCertificate(alias);
            certificates.add(certificate);
        }

        return new AuthenticationResponseValidator(certificates.toArray(new X509Certificate[0]));
    }

}
