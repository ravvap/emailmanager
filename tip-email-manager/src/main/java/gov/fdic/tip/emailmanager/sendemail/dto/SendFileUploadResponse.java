package gov.fdic.tip.emailmanager.sendemail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SendFileUploadResponse {
    private String uploadSessionToken;
    private String fileName;
    private int validRowCount;
    private int skippedRowCount;
    private List<String> skippedRowErrors;
}