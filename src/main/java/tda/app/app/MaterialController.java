package tda.app.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import tda.app.app.feed.FeedService;

@RestController
@RequestMapping("/api/courses")
public class MaterialController {

    private static final Path UPLOAD_ROOT = Paths.get("uploads");
    private static final Path INDEX_FILE = UPLOAD_ROOT.resolve("materials.json");
    private static final long MAX_FILE_BYTES = 30L * 1024L * 1024L; // 30 MB (Fáze 2)
    // IMPORTANT: must support java.time.Instant for persistence (findAndRegisterModules)
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, List<Material>> MATERIALS_BY_COURSE = new HashMap<>();

    private final FeedService feed;

    public MaterialController(FeedService feed) {
        this.feed = feed;
    }

    public record LinkRequest(String title, String description, String url) {}
    public record UpdateRequest(String title, String description, String url) {}

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(UPLOAD_ROOT);
            loadIndexFromDisk();
        } catch (Exception e) {
            System.err.println("WARN: Nelze načíst materials.json: " + e.getMessage());
        }
    }

    private void requireLecturer(String authHeader) {
        if (!Auth.isBearerValid(authHeader)) {
            throw new Unauthorized();
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}

    @ExceptionHandler(Unauthorized.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> unauthorized() {
        return Map.of("error", "Unauthorized");
    }

    // =========================
    // LIST
    // =========================
    @GetMapping("/{courseId}/materials")
    public List<Material> list(@PathVariable String courseId) {
        synchronized (MATERIALS_BY_COURSE) {
            List<Material> list = new ArrayList<>(MATERIALS_BY_COURSE.getOrDefault(courseId, List.of()));
            list.sort((a, b) -> {
                try {
                    return b.createdAt().compareTo(a.createdAt());
                } catch (Exception e) {
                    return 0;
                }
            });
            return list;
        }
    }

    // =========================
    // ADD LINK (lecturer only)
    // =========================
    @PostMapping(value = "/{courseId}/materials/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Material addLink(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @RequestBody LinkRequest req
    ) throws IOException {
        requireLecturer(auth);

        if (req == null) throw new IllegalArgumentException("Chybí data.");
        String title = req.title() == null ? "" : req.title().trim();
        String description = req.description() == null ? "" : req.description().trim();
        String url = req.url() == null ? "" : req.url().trim();

        if (title.isEmpty()) throw new IllegalArgumentException("Chybí title.");
        if (url.isEmpty()) throw new IllegalArgumentException("Chybí url.");

        // allow user to paste without scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Material m = new Material(
                UUID.randomUUID().toString(),
                courseId,
                MaterialType.LINK,
                title,
                description,
                Instant.now(),
                url,
                null,
                null,
                null,
                null,
                0L
        );

        synchronized (MATERIALS_BY_COURSE) {
            MATERIALS_BY_COURSE.computeIfAbsent(courseId, k -> new ArrayList<>()).add(m);
            saveIndexToDisk();
        }

        // Fáze 4 – automaticky generovaná událost do feedu
        feed.createAutoEvent(courseId, "Přidán nový odkaz: " + title);

        return m;
    }

    // =========================
    // UPDATE (lecturer only)
    // =========================
    @PutMapping(value = "/{courseId}/materials/{materialId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Material update(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String materialId,
            @RequestBody UpdateRequest req
    ) throws IOException {

        requireLecturer(auth);
        if (req == null) throw new IllegalArgumentException("Chybí data.");

        synchronized (MATERIALS_BY_COURSE) {
            List<Material> list = MATERIALS_BY_COURSE.getOrDefault(courseId, new ArrayList<>());

            int idx = -1;
            for (int i = 0; i < list.size(); i++) {
                if (Objects.equals(list.get(i).id(), materialId)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                throw new IllegalArgumentException("Materiál nebyl nalezen.");
            }

            Material old = list.get(idx);
            String newTitle = (req.title() == null) ? old.title() : req.title().trim();
            String newDesc  = (req.description() == null) ? old.description() : req.description().trim();

            if (newTitle == null || newTitle.isBlank()) {
                throw new IllegalArgumentException("Title nesmí být prázdný.");
            }

            Material updated;
            if (old.type() == MaterialType.LINK) {
                String newUrl = (req.url() == null) ? old.url() : req.url().trim();
                if (newUrl == null || newUrl.isBlank()) {
                    throw new IllegalArgumentException("URL nesmí být prázdné.");
                }
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "https://" + newUrl;
                }

                updated = new Material(
                        old.id(),
                        old.courseId(),
                        old.type(),
                        newTitle,
                        newDesc,
                        old.createdAt(),
                        newUrl,
                        old.faviconUrl(),
                        null,
                        null,
                        null,
                        0L
                );
            } else {
                updated = new Material(
                        old.id(),
                        old.courseId(),
                        old.type(),
                        newTitle,
                        newDesc,
                        old.createdAt(),
                        null,
                        old.faviconUrl(),
                        old.originalFilename(),
                        old.storedFilename(),
                        old.contentType(),
                        old.sizeBytes()
                );
            }

            list.set(idx, updated);
            MATERIALS_BY_COURSE.put(courseId, list);
            saveIndexToDisk();
            return updated;
        }
    }

    // =========================
    // DELETE (lecturer only)
    // =========================
    @DeleteMapping("/{courseId}/materials/{materialId}")
    public Map<String, Object> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @PathVariable String materialId
    ) throws IOException {

        requireLecturer(auth);

        synchronized (MATERIALS_BY_COURSE) {
            List<Material> list = new ArrayList<>(MATERIALS_BY_COURSE.getOrDefault(courseId, List.of()));
            Material found = null;
            for (Material m : list) {
                if (Objects.equals(m.id(), materialId)) { found = m; break; }
            }
            if (found == null) {
                throw new IllegalArgumentException("Materiál nebyl nalezen.");
            }

            list.removeIf(m -> Objects.equals(m.id(), materialId));
            MATERIALS_BY_COURSE.put(courseId, list);

            // if it is a file, also delete it from disk
            if (found.type() == MaterialType.FILE && found.storedFilename() != null && !found.storedFilename().isBlank()) {
                Path filePath = UPLOAD_ROOT.resolve(courseId).resolve(found.storedFilename()).normalize();
                try {
                    Files.deleteIfExists(filePath);
                } catch (Exception ignored) {}
            }

            saveIndexToDisk();
            return Map.of(
                    "message", "Deleted",
                    "materialId", materialId,
                    "courseId", courseId
            );
        }
    }

    // =========================
    // UPLOAD (lecturer only)
    // =========================
    @PostMapping(value = "/{courseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Material upload(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String courseId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        requireLecturer(auth);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Chybí title.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Soubor je prázdný.");
        }

        // Fáze 2 – max 30 MB na jeden soubor
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Soubor je příliš velký (max 30 MB).");
        }

        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        String ext = FileRules.extOf(original);
        if (!FileRules.ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Nepovolený typ souboru: " + ext);
        }

        Path courseDir = UPLOAD_ROOT.resolve(courseId);
        Files.createDirectories(courseDir);

        String safeOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String stored = UUID.randomUUID() + "_" + safeOriginal;
        Path target = courseDir.resolve(stored);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Material m = new Material(
                UUID.randomUUID().toString(),
                courseId,
                MaterialType.FILE,
                title.trim(),
                description == null ? "" : description.trim(),
                Instant.now(),
                null,
                null,
                original,
                stored,
                file.getContentType(),
                file.getSize()
        );

        synchronized (MATERIALS_BY_COURSE) {
            MATERIALS_BY_COURSE.computeIfAbsent(courseId, k -> new ArrayList<>()).add(m);
            saveIndexToDisk();
        }

        // Fáze 4 – automaticky generovaná událost do feedu
        feed.createAutoEvent(courseId, "Přidán nový materiál: " + title.trim());

        return m;
    }

    // =========================
    // DOWNLOAD
    // =========================
    @GetMapping("/{courseId}/materials/{materialId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String courseId,
            @PathVariable String materialId
    ) {
        try {
            Material m;
            synchronized (MATERIALS_BY_COURSE) {
                m = MATERIALS_BY_COURSE
                        .getOrDefault(courseId, List.of())
                        .stream()
                        .filter(x -> Objects.equals(x.id(), materialId))
                        .findFirst()
                        .orElse(null);
            }

            if (m == null || m.storedFilename() == null || m.storedFilename().isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String stored = m.storedFilename()
                    .replace("..", "")
                    .replace("/", "_")
                    .replace("\\", "_");

            Path filePath = UPLOAD_ROOT.resolve(courseId).resolve(stored).normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());

            String filename = (m.originalFilename() != null && !m.originalFilename().isBlank())
                    ? m.originalFilename()
                    : "material";

            MediaType ct = MediaType.APPLICATION_OCTET_STREAM;
            try {
                if (m.contentType() != null && !m.contentType().isBlank()) {
                    ct = MediaType.parseMediaType(m.contentType());
                }
            } catch (Exception ignored) { }

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(filename, UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(ct)
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================
    // PERSISTENCE
    // =========================
    private void loadIndexFromDisk() throws IOException {
        if (!Files.exists(INDEX_FILE)) return;

        String json = Files.readString(INDEX_FILE);
        Map<String, List<Material>> loaded =
                MAPPER.readValue(json, new TypeReference<Map<String, List<Material>>>() {});

        synchronized (MATERIALS_BY_COURSE) {
            MATERIALS_BY_COURSE.clear();
            if (loaded != null) MATERIALS_BY_COURSE.putAll(loaded);
        }
    }

    private void saveIndexToDisk() throws IOException {
        Path tmp = UPLOAD_ROOT.resolve("materials.json.tmp");
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MATERIALS_BY_COURSE);

        Files.writeString(tmp, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, INDEX_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, INDEX_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // =========================
    // ERRORS
    // =========================
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> badRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
