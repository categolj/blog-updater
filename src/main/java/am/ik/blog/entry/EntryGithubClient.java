package am.ik.blog.entry;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import am.ik.blog.BlogUpdaterProps;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Component
@RequiredArgsConstructor
public class EntryGithubClient {
	private final WebClient webClient = WebClient.builder()
			.baseUrl("https://api.github.com/repos/making/blog.ik.am")
			.defaultHeader(HttpHeaders.USER_AGENT, "am.ik.blog.BlogApiApplication")
			.build();
	private final EntryFactory entryFactory = new EntryFactory();
	private final ConcurrentMap<EntryId, Tuple2<LastModified, Entry>> lastModifieds = new ConcurrentHashMap<>();
	private final BlogUpdaterProps props;

	public Mono<Entry> get(EntryId entryId) {
		HttpHeaders headers = new HttpHeaders();
		if (!StringUtils.isEmpty(props.getGithubToken())) {
			headers.add(HttpHeaders.AUTHORIZATION, "token " + props.getGithubToken());
		}
		return webClient.get()
				.uri("/contents/content/{id}.md", format("%05d", entryId.value))
				.headers(headers)
				.ifModifiedSince(lastModifieds
						.getOrDefault(entryId, Tuples.of(LastModified.EPOCH, null))
						.getT1().value)
				.exchange().then(response -> {
					LastModified lastModified = new LastModified(
							response.headers().asHttpHeaders().getLastModified());
					if (response.statusCode() == HttpStatus.NOT_MODIFIED) {
						return Mono.just(lastModifieds.get(entryId).getT2());
					}
					if (response.statusCode() == HttpStatus.NOT_FOUND) {
						return Mono.empty();
					}
					if (response.statusCode().is4xxClientError()) {
						return Mono.error(new IllegalStateException(
								response.statusCode() + " CLIENT ERROR"));
					}
					if (response.statusCode().is5xxServerError()) {
						return Mono.error(new IllegalStateException(
								response.statusCode() + " SERVER ERROR"));
					}
					return response.bodyToMono(JsonNode.class)
							.map(node -> node.get("content").asText()).map(this::decode)
							.then(body -> bodyToBuilder(entryId, body))
							.then(builder -> builderToEntry(entryId, builder))
							.map(Entry::useFrontMatterDate)
							.doOnSuccess(entry -> lastModifieds.put(entryId,
									Tuples.of(lastModified, entry)));
				});
	}

	private InputStream decode(String base64Content) {
		return new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64Content));
	}

	private Mono<Entry.EntryBuilder> bodyToBuilder(EntryId entryId, InputStream body) {
		return Mono.justOrEmpty(entryFactory.parseBody(entryId, body));
	}

	private Mono<Entry> builderToEntry(EntryId entryId, Entry.EntryBuilder builder) {
		return author(entryId).map(t -> builder.created(t.getT1()).updated(t.getT2()))
				.map(Entry.EntryBuilder::build);
	}

	private Mono<Tuple2<Author, Author>> author(EntryId entryId) {
		Flux<JsonNode> commits = commits(entryId);
		Mono<Author> updated = commits.next().map(this::toAuthor);
		Mono<Author> created = commits.last().map(this::toAuthor);
		return created.and(updated);
	}

	private Author toAuthor(JsonNode node) {
		JsonNode author = node.get("commit").get("author");
		return new Author(new Name(author.get("name").asText()),
				new EventTime(OffsetDateTime.parse(author.get("date").asText())));
	}

	private Flux<JsonNode> commits(EntryId entryId) {
		return webClient.get()
				.uri("/commits?path={path}", format("content/%05d.md", entryId.value))
				.exchange().then(response -> response.bodyToMono(JsonNode.class))
				.flatMap(node -> Flux.fromStream(elements(node)));
	}

	private Stream<JsonNode> elements(JsonNode node) {
		return StreamSupport.stream(
				Spliterators.spliterator(node.elements(), node.size(), Spliterator.SIZED),
				false);
	}
}
