package tda.app.app.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedItemRepository extends JpaRepository<FeedItemEntity, String> {
    List<FeedItemEntity> findByCourseIdOrderByCreatedAtDesc(String courseId);
}
