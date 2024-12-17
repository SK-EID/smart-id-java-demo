package ee.sk.siddemo.model;

import jakarta.validation.constraints.NotNull;

public class UserDocumentNumberRequest {

    @NotNull
    private String documentNumber;

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}
