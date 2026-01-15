import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiController {

    // Vitest expects the endpoint to work on /api/ (with a trailing slash).
    // To be robust, we serve both /api and /api/.
    @GetMapping({"/api", "/api/"})
    public ResponseEntity<Map<String, String>> apiRoot(
            // @RequestParam(required = false) nám umožní zkontrolovat, zda data přišla
            @RequestParam(required = false) String validation
    ) {
        
        // ZDE JE LOGIKA PRO CHYBU 400:
        // Pokud například chybí parametr "validation", vrátíme 400 Bad Request.
        // Upravte podmínku (validation == null) podle toho, co přesně test vyžaduje.
        if (validation == null) {
            return ResponseEntity.badRequest().build(); // Toto pošle HTTP 400
        }

        // Pokud je vše v pořádku, pošle HTTP 200 a JSON
        return ResponseEntity.ok(Map.of("organization", "Student Cyber Games"));
    }
}
