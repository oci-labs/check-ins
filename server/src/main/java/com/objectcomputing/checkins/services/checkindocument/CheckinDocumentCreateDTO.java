package com.objectcomputing.checkins.services.checkindocument;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Introspected
public class CheckinDocumentCreateDTO {

    @NotBlank
    @Schema(required = true, description = "id of the associated checkIn")
    private UUID checkinsId;

    @NotBlank
    @Schema(required = true, description = "id of the uploaded document")
    private String uploadDocId;

    public UUID getCheckinsId() {
        return checkinsId;
    }

    public void setCheckinsId(UUID checkinsId) {
        this.checkinsId = checkinsId;
    }

    public String getUploadDocId() {
        return uploadDocId;
    }

    public void setUploadDocId(String uploadDocId) {
        this.uploadDocId = uploadDocId;
    }
}
