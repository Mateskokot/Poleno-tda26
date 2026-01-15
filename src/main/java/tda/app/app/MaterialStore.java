package tda.app.app;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MaterialStore {

    // materialId -> Material
    private final Map<String, Material> byId = new ConcurrentHashMap<>();

    // courseId -> list of materialIds
    private final Map<String, List<String>> byCourse = new ConcurrentHashMap<>();

    public List<Material> listByCourseNewestFirst(String courseId) {
        List<String> ids = byCourse.getOrDefault(courseId, List.of());
        List<Material> mats = new ArrayList<>();
        for (String id : ids) {
            Material m = byId.get(id);
            if (m != null) mats.add(m);
        }
        mats.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return mats;
    }

    // ===== pomocné CRUD =====

    public Material findById(String id) {
        return byId.get(id);
    }

    public Material add(Material m) {
        byId.put(m.id(), m);
        byCourse.computeIfAbsent(m.courseId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(m.id());
        return m;
    }

    public Material deleteť(String id) { // (nepoužívej; jen pro kompatibilitu pokud něco volá staré jméno)
        return delete(id);
    }

    public Material delete(String id) {
        Material m = byId.remove(id);
        if (m != null) {
            List<String> ids = byCourse.get(m.courseId());
            if (ids != null) ids.remove(id);
        }
        return m;
    }

    // ===== API pro controller (tohle volá MaterialController) =====

    public Material addLink(String courseId, String title, String description, String url) {
        Material m = new Material(
                UUID.randomUUID().toString(), // id
                courseId,
                MaterialType.LINK,
                title,
                description,
                Instant.now(),
                url,
                null,   // faviconUrl
                null,   // originalFilename
                null,   // storedFilename
                null,   // contentType
                0L      // sizeBytes
        );
        return add(m);
    }

    public Material addFile(
            String courseId,
            String title,
            String description,
            String originalFilename,
            String storedFilename,
            String contentType,
            long sizeBytes
    ) {
        Material m = new Material(
                UUID.randomUUID().toString(), // id
                courseId,
                MaterialType.FILE,
                title,
                description,
                Instant.now(),
                null,          // url
                null,          // faviconUrl
                originalFilename,
                storedFilename,
                contentType,
                sizeBytes
        );
        return add(m);
    }
}
