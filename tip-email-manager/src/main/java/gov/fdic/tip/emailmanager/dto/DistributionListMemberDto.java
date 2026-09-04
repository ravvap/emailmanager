package gov.fdic.tip.emailmanager.dto;

import lombok.Data;

@Data
public class DistributionListMemberDto {
    private Long contactId;
    private String name;
    private String email;
    private String status;
}