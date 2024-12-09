package ee.sk.siddemo.model;

import java.net.URI;

public class DynamicContent {

    private final URI dynamicLink;
    private final String qrCode;

    public DynamicContent(URI dynamicLink, String qrDataUri) {
        this.dynamicLink = dynamicLink;
        this.qrCode = qrDataUri;
    }

    public URI getDynamicLink() {
        return dynamicLink;
    }

    public String getQrCode() {
        return qrCode;
    }
}
