package gov.fdic.tip.emailmanager.sendemail.dto;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.BounceCategory;
import lombok.Data;

@Data
public class DispatchResultCallbackRequest {
    private Long sendRecipientId;
    private boolean delivered;
    private BounceCategory bounceCategory;
    private String failureReason;
}