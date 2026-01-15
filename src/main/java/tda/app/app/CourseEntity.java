package tda.app.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entita pro kurzy (Fáze 1).
 *
 * Primární klíč je String UUID, aby se snadno používal v URL (/courses/{uuid}).
 */
@Entity
@Table(name = "courses")
public class CourseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String lecturer;

    protected CourseEntity() {
        // JPA
    }

    public CourseEntity(String id, String title, String description, String lecturer) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.lecturer = lecturer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }
}
