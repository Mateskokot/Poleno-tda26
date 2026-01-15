package tda.app.app.feed;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class FeedService {

    public record FeedItemDto(
            String id,
            String courseId,
            FeedItemType type,
            String message,
            Instant createdAt,
            Instant updatedAt,
            boolean edited
    ) {}

    private final FeedItemRepository repo;

    // active SSE connections per course
    private final Map<String, Set<SseEmitter>> emittersByCourse = new ConcurrentHashMap<>();

    public FeedService(FeedItemRepository repo) {
        this.repo = repo;
    }

    public List<FeedItemDto> list(String courseId) {
        return repo.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public SseEmitter subscribe(String courseId) {
        // 0L = no timeout; keep it simple for TdA local testing
        SseEmitter emitter = new SseEmitter(0L);

        emittersByCourse.computeIfAbsent(courseId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> cleanup(courseId, emitter));
        emitter.onTimeout(() -> cleanup(courseId, emitter));
        emitter.onError((ex) -> cleanup(courseId, emitter));

        // send an initial event so the client knows the connection is alive
        try {
            emitter.send(SseEmitter.event()
                    .name("hello")
                    .data(Map.of("ok", true, "courseId", courseId), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            cleanup(courseId, emitter);
        }

        return emitter;
    }

    private void cleanup(String courseId, SseEmitter emitter) {
        try {
            Set<SseEmitter> set = emittersByCourse.get(courseId);
            if (set != null) {
                set.remove(emitter);
                if (set.isEmpty()) emittersByCourse.remove(courseId);
            }
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public FeedItemDto createLecturerPost(String courseId, String message) {
        String msg = (message == null) ? "" : message.trim();
        if (msg.isBlank()) {
            throw new IllegalArgumentException("Zpráva nesmí být prázdná.");
        }
        if (msg.length() > 4000) {
            msg = msg.substring(0, 4000);
        }

        Instant now = Instant.now();
        FeedItemEntity e = new FeedItemEntity(
                UUID.randomUUID().toString(),
                courseId,
                FeedItemType.POST,
                msg,
                now,
                now,
                false
        );
        repo.save(e);

        FeedItemDto dto = toDto(e);
        broadcast(courseId, dto);
        return dto;
    }

    @Transactional
    public FeedItemDto createAutoEvent(String courseId, String message) {
        String msg = (message == null) ? "" : message.trim();
        if (msg.isBlank()) return null;
        if (msg.length() > 4000) msg = msg.substring(0, 4000);

        Instant now = Instant.now();
        FeedItemEntity e = new FeedItemEntity(
                UUID.randomUUID().toString(),
                courseId,
                FeedItemType.AUTO,
                msg,
                now,
                now,
                false
        );
        repo.save(e);

        FeedItemDto dto = toDto(e);
        broadcast(courseId, dto);
        return dto;
    }

    @Transactional
    public FeedItemDto updateLecturerPost(String courseId, String id, String message) {
        FeedItemEntity e = repo.findById(id).orElse(null);
        if (e == null || !Objects.equals(e.getCourseId(), courseId)) {
            throw new IllegalArgumentException("Příspěvek nebyl nalezen.");
        }
        if (e.getType() != FeedItemType.POST) {
            throw new IllegalArgumentException("Automatické události nelze upravovat.");
        }
        String msg = (message == null) ? "" : message.trim();
        if (msg.isBlank()) {
            throw new IllegalArgumentException("Zpráva nesmí být prázdná.");
        }
        if (msg.length() > 4000) msg = msg.substring(0, 4000);

        e.setMessage(msg);
        e.setEdited(true);
        e.setUpdatedAt(Instant.now());
        repo.save(e);

        FeedItemDto dto = toDto(e);
        broadcast(courseId, Map.of(
                "type", "update",
                "item", dto
        ));
        return dto;
    }

    @Transactional
    public void deleteLecturerPost(String courseId, String id) {
        FeedItemEntity e = repo.findById(id).orElse(null);
        if (e == null || !Objects.equals(e.getCourseId(), courseId)) {
            throw new IllegalArgumentException("Příspěvek nebyl nalezen.");
        }
        if (e.getType() != FeedItemType.POST) {
            throw new IllegalArgumentException("Automatické události nelze mazat.");
        }
        repo.deleteById(id);
        broadcast(courseId, Map.of(
                "type", "delete",
                "id", id
        ));
    }

    private FeedItemDto toDto(FeedItemEntity e) {
        return new FeedItemDto(
                e.getId(),
                e.getCourseId(),
                e.getType(),
                e.getMessage(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.isEdited()
        );
    }

    private void broadcast(String courseId, FeedItemDto dto) {
        broadcast(courseId, Map.of(
                "type", "new",
                "item", dto
        ));
    }

    private void broadcast(String courseId, Object payload) {
        Set<SseEmitter> set = emittersByCourse.get(courseId);
        if (set == null || set.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : set) {
            try {
                emitter.send(SseEmitter.event()
                        .name("feed")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        for (SseEmitter d : dead) {
            cleanup(courseId, d);
        }
    }
}
