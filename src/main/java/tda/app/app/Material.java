package tda.app.app;

import java.time.Instant;

public record Material(
        String id,
        String courseId,
        MaterialType type,
        String title,
        String description,
        Instant createdAt,

        // LINK
        String url,
        String faviconUrl,

        // FILE
        String originalFilename,
        String storedFilename,
        String contentType,
        long sizeBytes
) {}
