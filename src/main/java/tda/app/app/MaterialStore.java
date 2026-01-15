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
        String normalizedUrl = normalizeUrl(url);
        String favicon = deriveFaviconUrl(normalizedUrl);
        Material m = new Material(
                UUID.randomUUID().toString(), // id
                courseId,
                MaterialType.LINK,
                title,
                description,
                Instant.now(),
                normalizedUrl,
                favicon,
                null,   // originalFilename
                null,   // storedFilename
                null,   // contentType
                0L      // sizeBytes
        );
        return add(m);
    }

    private static String normalizeUrl(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.isEmpty()) return u;
        // Pokud uživatel zadá doménu bez schématu, doplníme https://
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        return u;
    }

    /**
     * Jednoduchá favicon strategie: https://HOST/favicon.ico
     *
     * Zadání Fáze 2 očekává favicon u odkazů. Pro jednoduchost neparsujeme HTML.
     */
    private static String deriveFaviconUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) return null;
            String scheme = uri.getScheme();
            if (scheme == null || scheme.isBlank()) scheme = "https";
            return scheme + "://" + host + "/favicon.ico";
        } catch (Exception _ex) {
            return null;
        }
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
