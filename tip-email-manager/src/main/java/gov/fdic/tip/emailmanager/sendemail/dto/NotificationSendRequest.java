package gov.fdic.tip.emailmanager.sendemail.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSendRequest {
    private String emailAddress;
    private String subject;
    private String body;
}