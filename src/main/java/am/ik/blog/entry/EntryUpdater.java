package am.ik.blog.entry;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EntryUpdater {
	private final EntryGithubClient githubClient;
	private final EntryMapperReactiveWrapper entryMapper;

	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(POST("/entries/{entryId}"), this::add)
				.andRoute(GET("/entries/{entryId}"), this::get)
				.andRoute(PUT("/entries/{entryId}"), this::update);
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
		return getAndSave(entryId).transform(e -> created(req.uri()).body(fromObject(e)))
				.otherwiseIfEmpty(notFound().build());
	}

	Mono<ServerResponse> update(ServerRequest req) {
		EntryId entryId = entryId(req);
		return getAndSave(entryId).transform(e -> ok().body(fromObject(e)))
				.otherwiseIfEmpty(notFound().build());
	}

	Mono<ServerResponse> get(ServerRequest req) {
		EntryId entryId = entryId(req);
		return entryMapper.findOne(entryId).transform(e -> ok().body(fromObject(e)))
				.otherwiseIfEmpty(notFound().build());
	}

}
