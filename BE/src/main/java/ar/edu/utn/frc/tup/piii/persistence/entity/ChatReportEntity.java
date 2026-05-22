package ar.edu.utn.frc.tup.piii.persistence.entity;

import ar.edu.utn.frc.tup.piii.dtos.ChatMessageResponse;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", nullable = false)
    private UserEntity reported;

    @Column(nullable = false)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_history", nullable = false)
    private List<ChatMessageResponse> chatHistory;

    @Column(name = "is_validated", nullable = false)
    private Boolean isValidated;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
