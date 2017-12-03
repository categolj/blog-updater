package am.ik.blog.entry;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import am.ik.blog.BlogUpdaterProps;
import am.ik.blog.entry.factory.EntryFactory;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Component
@RequiredArgsConstructor
public class EntryGithubClient {
	private final Logger log = LoggerFactory.getLogger(EntryGithubClient.class);
	private final WebClient webClient = WebClient.builder()
			.baseUrl("https://api.github.com/repos/")
			.defaultHeader(HttpHeaders.USER_AGENT, "am.ik.blog.BlogApiApplication")
			.build();
	private final EntryFactory entryFactory = new EntryFactory();
	private final ConcurrentMap<EntryId, Tuple2<LastModified, Entry>> lastModifieds = new ConcurrentHashMap<>();
	private final BlogUpdaterProps props;

	Consumer<HttpHeaders> headers(String repository) {
		return headers -> {
			String token = Optional
					.ofNullable(props.getGithubToken())
					.map(m -> m.get(repository))
					.orElseGet(
							() -> {
								String key = "BLOG_UPDATER_GITHUB_TOKEN_"
										+ (repository.toUpperCase().replace("/", "_")
												.replace(".", "_"));
								log.warn("fallback to get from environment variable({})",
										key);
								return System.getenv(key);
							});
			if (!StringUtils.isEmpty(token)) {
				log.info("Set Github Token for {}", repository);
				headers.add(HttpHeaders.AUTHORIZATION, "token " + token);
			}
		};
	}

	public Mono<Entry> get(String repository, EntryId entryId) {
		log.info("get repository={}, entryId={}", repository, entryId);
		return webClient.get()
				.uri(repository + "/contents/content/{id}.md",
						format("%05d", entryId.value))
				.headers(headers(repository))
				.ifModifiedSince(lastModifieds
						.getOrDefault(entryId, Tuples.of(LastModified.EPOCH, Entry.builder().build()))
						.getT1().value)
				.exchange().flatMap(response -> {
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
							.flatMap(body -> bodyToBuilder(entryId, body))
							.flatMap(builder -> builderToEntry(repository, entryId,
									builder))
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

	private Mono<Entry> builderToEntry(String repository, EntryId entryId,
			Entry.EntryBuilder builder) {
		return author(repository, entryId)
				.map(t -> builder.created(t.getT1()).updated(t.getT2()))
				.map(Entry.EntryBuilder::build);
	}

	private Mono<Tuple2<Author, Author>> author(String repository, EntryId entryId) {
		Flux<JsonNode> commits = commits(repository, entryId);
		Mono<Author> updated = commits.next().map(this::toAuthor);
		Mono<Author> created = commits.last().map(this::toAuthor);
		return Mono.zip(updated, created);
	}

	private Author toAuthor(JsonNode node) {
		JsonNode author = node.get("commit").get("author");
		return new Author(new Name(author.get("name").asText()),
				new EventTime(OffsetDateTime.parse(author.get("date").asText())));
	}

	private Flux<JsonNode> commits(String repository, EntryId entryId) {
		log.info("commits repository={}, entryId={}", repository, entryId);
		return webClient.get()
				.uri(repository + "/commits?path={path}",
						format("content/%05d.md", entryId.value))
				.headers(headers(repository)).exchange()
				.flatMap(response -> response.bodyToMono(JsonNode.class))
				.flatMapMany(node -> Flux
						.fromStream(StreamSupport.stream(node.spliterator(), false)));
	}
}
