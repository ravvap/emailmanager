package gov.fdic.tip.emailmanager.sendemail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "em_email_send_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "send_attachment_id")
    private Long sendAttachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "send_id", nullable = false)
    private EmailSend emailSend;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_extension", nullable = false)
    private String fileExtension;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Lob
    @Column(name = "file_content", nullable = false)
    private byte[] fileContent;
}