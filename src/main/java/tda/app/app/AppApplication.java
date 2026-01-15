package tda.app.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {
		// Ensure local persistence directories exist BEFORE Spring initializes the H2 datasource.
		// (H2 file-based DB configured to ./uploads/feeddb requires ./uploads to exist.)
		try {
			Files.createDirectories(Path.of("uploads"));
		} catch (IOException e) {
			// If this fails, Spring will likely fail later as well; print a clear message.
			System.err.println("Failed to create required directory 'uploads': " + e.getMessage());
		}

		SpringApplication.run(AppApplication.class, args);
	}

}
