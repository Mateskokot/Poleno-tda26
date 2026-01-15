package tda.app.app.feed;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "course_feed")
public class FeedItemEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false)
    private String courseId;

    @Enumerated(EnumType.STRING)
    // "type" is a reserved keyword in some SQL dialects (including H2 in certain modes).
    // Using an explicit, non-keyword column name avoids schema-generation failures at startup.
    @Column(name = "item_type", nullable = false, length = 16)
    private FeedItemType type;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    // timestamp of the last edit (optional; equals createdAt for new items)
    @Column
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean edited;

    protected FeedItemEntity() {
        // JPA
    }

    public FeedItemEntity(String id,
                          String courseId,
                          FeedItemType type,
                          String message,
                          Instant createdAt,
                          Instant updatedAt,
                          boolean edited) {
        this.id = id;
        this.courseId = courseId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.edited = edited;
    }

    public String getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public FeedItemType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }
}
