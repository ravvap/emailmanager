package gov.fdic.tip.emailmanager.sendemail.port;

public interface EmailDispatchEnginePort {
    void enqueueSend(Long sendId);
}