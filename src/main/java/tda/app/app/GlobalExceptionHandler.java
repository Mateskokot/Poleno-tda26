package tda.app.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

/**
 * Jednotné mapování chyb na 400/401 pro manuální testování TdA.
 *
 * Pozn.: Některé controllery už mají své vlastní @ExceptionHandler.
 * Tento handler pokrývá zejména multipart limity a běžné validace.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<Map<String, String>> uploadTooLarge(Exception _ex) {
        // Požadavek Fáze 2: při překročení limitu vrátit 400.
        return ResponseEntity.badRequest().body(Map.of("error", "Soubor je příliš velký (max 30 MB)."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage() == null ? "Bad Request" : ex.getMessage()));
    }
}
