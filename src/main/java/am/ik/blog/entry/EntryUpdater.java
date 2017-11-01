package am.ik.blog.entry;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntryUpdater {
	private final EntryGithubClient githubClient;
	private final EntryMapperReactiveWrapper entryMapper;
	private final ObjectMapper objectMapper;

	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(POST("/entries/{entryId}"), this::add)
				.andRoute(GET("/entries/{entryId}"), this::get)
				.andRoute(PUT("/entries/{entryId}"), this::update);
	}

	@StreamListener(target = Sink.INPUT, condition = "headers['type']=='added' || headers['type']=='modified'")
	void handleUpdate(@Payload String body) throws Exception {
		WebHookRequest request = this.objectMapper.readValue(body, WebHookRequest.class);
		log.info("Received {}", request);
		Mono.when(request.getEntryIds().stream()
				.map(entryId -> this.getAndSave(request.getRepository(), entryId).then())
				.collect(toList())).doOnSuccess(o -> log.info("Updated!"))
				.doOnError(e -> log.error("Error!", e)).subscribe();
	}

	@StreamListener(target = Sink.INPUT, condition = "headers['type']=='removed'")
	void handleDelete(@Payload String body) throws Exception {
		WebHookRequest request = this.objectMapper.readValue(body, WebHookRequest.class);
		log.info("Received {}", request);
		Mono.when(
				request.getEntryIds().stream().map(entryMapper::delete).collect(toList()))
				.doOnSuccess(o -> log.info("Removed!"))
				.doOnError(e -> log.error("Error!", e)).subscribe();
	}

	EntryId entryId(ServerRequest req) {
		return new EntryId(Long.valueOf(req.pathVariable("entryId")));
	}

	Mono<Entry> getAndSave(String repository, EntryId entryId) {
		Mono<Entry> entry = githubClient.get(repository, entryId);
		return entry.flatMap(e -> entryMapper.save(entry).then(entry));
	}

	Mono<ServerResponse> add(ServerRequest req) {
		EntryId entryId = entryId(req);
		return getAndSave(req.queryParam("repository").orElse(""), entryId)
				.flatMap(e -> created(req.uri()).body(fromObject(e)))
				.switchIfEmpty(notFound().build());
	}

	Mono<ServerResponse> update(ServerRequest req) {
		EntryId entryId = entryId(req);
		return getAndSave(req.queryParam("repository").orElse(""), entryId)
				.flatMap(e -> ok().body(fromObject(e))).switchIfEmpty(notFound().build());
	}

	Mono<ServerResponse> get(ServerRequest req) {
		EntryId entryId = entryId(req);
		return entryMapper.findOne(entryId).flatMap(e -> ok().body(fromObject(e)))
				.switchIfEmpty(notFound().build());
	}

}
