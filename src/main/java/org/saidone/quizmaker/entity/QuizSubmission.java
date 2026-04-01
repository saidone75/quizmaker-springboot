package org.saidone.quizmaker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_submission_student_quiz", columnNames = {"student_id", "quiz_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmission {

    @Id
    @Column(name = "id", nullable = false)
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
    private List<Integer> answers;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Builder.Default
    @Column(name = "unlocked", nullable = false)
    private Boolean unlocked = false;

    @CreationTimestamp
    @Convert(converter = LocalDateTimeColumnConverter.class)
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
