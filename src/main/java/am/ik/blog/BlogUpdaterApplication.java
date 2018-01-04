package am.ik.blog;

import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import am.ik.blog.entry.EntryUpdater;

@SpringBootApplication
public class BlogUpdaterApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogUpdaterApplication.class, args);
	}

	@Bean
	Function<Map<String, Object>, String> entryUpdater() {
		return new EntryUpdater();
	}
}
