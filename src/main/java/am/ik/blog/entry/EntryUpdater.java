package am.ik.blog.entry;

import static java.util.stream.Collectors.toList;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntryUpdater {
	private final EntryGithubClient githubClient;
	private final EntryMapperReactiveWrapper entryMapper;

	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(POST("/entries/{entryId}"), this::add)
				.andRoute(GET("/entries/{entryId}"), this::get)
				.andRoute(PUT("/entries/{entryId}"), this::update);
	}

	@StreamListener(target = Sink.INPUT, condition = "headers['type']=='added' || headers['type']=='modified'")
	void handleUpdate(@Payload WebHookRequest request) {
		log.info("Received {}", request);
		Mono.when(request.getEntryIds().stream()
				.map(entryId -> this.getAndSave(entryId).then()).collect(toList()))
				.doOnSuccess(o -> log.info("Updated!"))
				.doOnError(e -> log.error("Error!", e)).subscribe();
	}

	@StreamListener(target = Sink.INPUT, condition = "headers['type']=='removed'")
	void handleDelete(@Payload WebHookRequest request) {
		log.info("Received {}", request);
		Mono.when(
				request.getEntryIds().stream().map(entryMapper::delete).collect(toList()))
				.doOnSuccess(o -> log.info("Removed!"))
				.doOnError(e -> log.error("Error!", e)).subscribe();
	}

	EntryId entryId(ServerRequest req) {
		return new EntryId(Long.valueOf(req.pathVariable("entryId")));
	}

	Mono<Entry> getAndSave(EntryId entryId) {
		Mono<Entry> entry = githubClient.get(entryId);
		return entry.then(() -> entryMapper.save(entry).then(entry));
	}

	Mono<ServerResponse> add(ServerRequest req) {
		EntryId entryId = entryId(req);
		return getAndSave(entryId).transform(e -> created(req.uri()).body(e, Entry.class))
				.switchIfEmpty(notFound().build());
	}

	Mono<ServerResponse> update(ServerRequest req) {
		EntryId entryId = entryId(req);
		return getAndSave(entryId).transform(e -> ok().body(e, Entry.class))
				.switchIfEmpty(notFound().build());
	}

	Mono<ServerResponse> get(ServerRequest req) {
		EntryId entryId = entryId(req);
		return entryMapper.findOne(entryId).transform(e -> ok().body(e, Entry.class))
				.switchIfEmpty(notFound().build());
	}

}
