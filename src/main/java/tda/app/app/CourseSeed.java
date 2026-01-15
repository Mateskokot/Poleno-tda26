package tda.app.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Seed dat pro kurzy.
 *
 * Hodnoticí prostředí může používat neperzistentní DB; seed zajišťuje, že aplikace
 * má po startu alespoň nějaké kurzy.
 */
@Configuration
public class CourseSeed {

    @Bean
    CommandLineRunner seedCourses(CourseRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            repo.save(new CourseEntity(
                    UUID.fromString("11111111-1111-1111-1111-111111111111").toString(),
                    "Základy HTML",
                    "Základní tagy, struktura stránky, odkazy, formuláře.",
                    "Lektor A"
            ));
            repo.save(new CourseEntity(
                    UUID.fromString("22222222-2222-2222-2222-222222222222").toString(),
                    "Úvod do JavaScriptu",
                    "Proměnné, podmínky, funkce, práce s DOM.",
                    "Lektor B"
            ));
        };
    }
}
