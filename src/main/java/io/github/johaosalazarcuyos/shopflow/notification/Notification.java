package io.github.johaosalazarcuyos.shopflow.notification;

import io.github.johaosalazarcuyos.shopflow.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;

/** Registro de cada notificación enviada (o intentada), útil para auditoría y reintentos. */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String recipient;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(length = 500)
    private String error;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(String recipient, String type, String subject, String body) {
        this.recipient = recipient;
        this.type = type;
        this.subject = subject;
        this.body = body;
    }

    void markSent(boolean viaEmail) { this.status = viaEmail ? NotificationStatus.SENT : NotificationStatus.LOGGED; }

    void markFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.error = error == null ? null : error.substring(0, Math.min(500, error.length()));
    }

    public String getRecipient() { return recipient; }
    public String getType() { return type; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
}
