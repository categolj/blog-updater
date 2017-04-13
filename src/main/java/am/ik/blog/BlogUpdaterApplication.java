package am.ik.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import am.ik.blog.entry.EntryUpdater;

@SpringBootApplication
@EnableBinding(Sink.class)
public class BlogUpdaterApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogUpdaterApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> route(EntryUpdater entryUpdater) {
		return entryUpdater.route();
	}
}