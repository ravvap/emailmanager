package gov.fdic.tip.emailmanager.sendemail.client;

import gov.fdic.tip.emailmanager.sendemail.dto.NotificationSendRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificationClient", url = "${notification.service.url:http://localhost:8080}")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/sendemail")
    void sendEmail(@RequestBody NotificationSendRequest request);
}