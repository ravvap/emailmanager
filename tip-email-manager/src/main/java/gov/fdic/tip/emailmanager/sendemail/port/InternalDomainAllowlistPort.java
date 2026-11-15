package gov.fdic.tip.emailmanager.sendemail.port;

import gov.fdic.tip.emailmanager.sendemail.constants.AppConstants.RecipientClassification;

public interface InternalDomainAllowlistPort {
    RecipientClassification classifyEmail(String emailAddress);
}