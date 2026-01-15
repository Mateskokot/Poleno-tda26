package tda.app.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    public record LoginRequest(String username, String password) {}

    @PostMapping("/api/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        if (req == null) throw new Unauthorized();

        boolean ok = "lecturer".equals(req.username()) && "TdA26!".equals(req.password());
        if (!ok) throw new Unauthorized();

        return Map.of("token", Auth.TOKEN);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class Unauthorized extends RuntimeException {}
}
