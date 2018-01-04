package am.ik.blog.entry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import am.ik.blog.entry.factory.EntryFactory;
import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;

public class EntryGithubClient {
	private final Logger log = LoggerFactory.getLogger(EntryGithubClient.class);
	private final HttpClient httpClient = HttpClient.create();
	private final EntryFactory entryFactory = new EntryFactory();
	private final ObjectMapper objectMapper;

	public EntryGithubClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private EntryGithubClient() {
		this(new ObjectMapper());
	}

	public Mono<Entry> get(String repo, EntryId entryId) {
		log.info("get repo={}, entryId={}", repo, entryId);
		String url = String.format(
				"https://api.github.com/repos/%s/contents/content/%05d.md", repo,
				entryId.value);
		return this.httpClient //
				.request(HttpMethod.GET, url, req -> {
					String r = (repo.toUpperCase().replace("/", "_").replace(".", "_"));
					String key = "BLOG_UPDATER_GITHUB_TOKEN_" + r;
					String token = System.getenv(key);
					if (token != null) {
						return req.addHeader("Authorization", "token " + token);
					}
					else {
						return req;
					}
				}) //
				.flatMap(r -> r.receive().aggregate().asByteArray()) //
				.map(this::toJsonNode) //
				.map(n -> n.get("content").asText()) //
				.map(this::decode) //
				.flatMap(body -> bodyToBuilder(entryId, body)) //
				.flatMap(builder -> builderToEntry(repo, entryId, builder)) //
				.map(Entry::useFrontMatterDate);
	}

	private JsonNode toJsonNode(byte[] b) {
		try {
			return this.objectMapper.readTree(b);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
		return Mono.zip(created, updated);
	}

	private Author toAuthor(JsonNode node) {
		JsonNode author = node.get("commit").get("author");
		return new Author(new Name(author.get("name").asText()),
				new EventTime(OffsetDateTime.parse(author.get("date").asText())));
	}

	private Flux<JsonNode> commits(String repository, EntryId entryId) {
		log.info("commits repository={}, entryId={}", repository, entryId);
		String url = String.format(
				"https://api.github.com/repos/%s/commits?path=content/%05d.md",
				repository, entryId.value);
		return this.httpClient.get(url) //
				.flatMap(r -> r.receive().aggregate().asByteArray()) //
				.map(this::toJsonNode) //
				.flatMapMany(node -> Flux
						.fromStream(StreamSupport.stream(node.spliterator(), false)));
	}
}
