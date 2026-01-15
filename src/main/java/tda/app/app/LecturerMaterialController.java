package tda.app.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/lecturer/courses")
public class LecturerMaterialController {

    @PostMapping(path = "/{courseId}/materials", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadMaterial(
            @PathVariable String courseId,                 // ✅ UUID jako String
            @RequestParam("title") String title,           // musí sedět s FormData: title
            @RequestParam("file") MultipartFile file,      // musí sedět s FormData: file
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // volitelné ověření tokenu (když chceš upload jen po přihlášení)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing title"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }

        // TODO: tady později ulož soubor (disk/DB) a přiřaď k courseId
        // Zatím jen potvrzení, že endpoint funguje.

        return ResponseEntity.status(201).body(Map.of(
                "message", "Uploaded",
                "courseId", courseId,
                "title", title,
                "filename", file.getOriginalFilename(),
                "size", file.getSize()
        ));
    }
}
