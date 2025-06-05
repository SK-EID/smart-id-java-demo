* Branch: `v3.1_GLASSFISH`

This branch demonstrates how to configure the `Smart-ID Java client` to use a custom `Glassfish Jersey client` (`org.glassfish.jersey.client.ClientConfig`).  
The goal is to test and provide a working example of overriding the default HTTP client configuration, for example to enable custom logging, timeouts, or advanced connection options.

## Goals of this branch:
* Add support for Glassfish Jersey `ClientConfig`
* Override Smart-ID Java client’s internal client with custom configuration
* Demonstrate how to inject JAX-RS client filters (e.g. logging via `JaxRsClientRequestLogger`)

# Smart-ID Java Demo

Sample application to demonstrate how to use [smart-id-java-client](https://github.com/SK-EID/smart-id-java-client) library and implement:
* authentication with [Smart-ID](https://github.com/SK-EID/smart-id-documentation)
* fetching the signing certificate and signing a document with [Smart-ID](https://github.com/SK-EID/smart-id-documentation) using [Digidoc4j library](https://github.com/open-eid/digidoc4j)

## How to start application

Option 1: `./mvnw spring-boot:run`

Option 2. run main method of `SmartIdJavaDemoApplication`


## How to use

Start the application, open [http://localhost:8081/](http://localhost:8081/)
and authenticate or sign a document using 
[test persons](https://github.com/SK-EID/smart-id-documentation/wiki/Environment-technical-parameters).

### How to run tests with a real phone

You need to register demo smart-id (And Testflight app if you have an IOS phone)

## Building a real-life application

For real-life use case you need to change in class `SmartIdSignatureServiceImpl` in method `sendSignatureRequest`

the following line (constructor parameter needs to be PROD):

        Configuration configuration = new Configuration(Configuration.Mode.PROD);

You also need to create your own Trust Store (or two separate Trust Stores)
and only import the certificates you trust:

  * SSL certificate of SK Smart-ID API endpoint. 
  * Smart-ID root certificates (to validate that the returned certificate is issued by SK). 
    * For this you need to import tests certificates into sid.trusted_root_certs.p12
      * TEST_of_EID-SK_2016.pem.crt 
      * TEST_of_NQ-SK_2016.pem.crt 
      * TEST_of_SK_ID_Solutions_EID-Q_2024E.pem.crt
      * TEST_EID-NQ_2021E.pem.crt

## Troubleshooting

### Error 'unable to find valid certification path to requested target'

This application only connects to servers it trusts. That is the SSL cert of the
server must be imported into file src/main/resources/sid.trusted_server_certs.p12.

If you change this application to connect to some other server 
(or if the SSL cert of the demo server has expired and replaced with new one)
then you need to import server's cert into the trust store.

More info how to do this can be found from [smart-id-java-clientdocumentation](https://github.com/SK-EID/smart-id-java-client).

## Trust Stores information

Demo application has two separate trust stores:
 * sid.trusted_server_certs.p12 holds SSL certificates of servers it trusts 
 * sid.trusted_root_certs.p12 holds all Smart-ID root certificates of Smart-ID test chain

Next section shows how these two trust stores were created
and with instructions how to create similar trust stores for production.

NB! Avoid placing certificates from production chain and test chain into
the same trust store. Create separate trust stores for each environment of your application
and only import certificates needed for that specific environment.

### Trust store for SSL certificates 
 
Without following step one would not be able to connect to Demo API server:
 * import demo env API endpoint SSL root certificate. 
 * Note that for demo we have imported ROOT certificate (DigiCert TLS RSA SHA256 2020 CA1) from the chain. Importing root certificate is not recommended for production.
    ```sh
      keytool -importcert -storetype PKCS12 -keystore sid.trusted_server_certs.p12 \
             -storepass changeit -alias sidDemoServerRootCert -file demo_root_cert.crt -noprompt
    ```

### Trust store for known Smart-ID certificates

First we create a trust store and import one of two test root certifices.
Without following this step you can't use any of the test users provided here
https://github.com/SK-EID/smart-id-documentation/wiki/Environment-technical-parameters#test-accounts-for-automated-testing

Commands to import demo env root certificates:
```sh
       keytool -importcert -storetype PKCS12 -keystore sid.trusted_root_certs.p12 \
        -storepass changeit -alias "TEST_of_EID-SK_2016" -file TEST_of_EID-SK_2016.pem.crt -noprompt
  
       keytool -importcert -storetype PKCS12 -keystore sid.trusted_root_certs.p12 \
              -storepass changeit -alias "TEST_of_NQ-SK_2016" -file TEST_of_NQ-SK_2016.pem.crt -noprompt

       keytool -importcert -storetype PKCS12 -keystore sid.trusted_root_certs.p12 \
        -storepass changeit -alias "TEST_of_SK_ID_Solutions_EID-Q_2024E" -file TEST_of_SK_ID_Solutions_EID-Q_2024E.pem.crt -noprompt

       keytool -importcert -storetype PKCS12 -keystore sid.trusted_root_certs.p12 \
              -storepass changeit -alias "TEST_EID-NQ_2021E" -file TEST_EID-NQ_2021E.pem.crt -noprompt
```

