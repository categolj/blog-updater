package am.ik.blog.entry;

import static java.util.stream.StreamSupport.stream;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import am.ik.blog.BlogUpdaterApplication;

public class EntryUpdater implements Function<Map<String, Object>, String> {
	private static final Logger log = LoggerFactory.getLogger(EntryUpdater.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	EntryMapper entryMapper;
	ConfigurableApplicationContext context;
	EntryGithubClient githubClient = new EntryGithubClient(objectMapper);

	@Override
	public String apply(Map<String, Object> body) {
		JsonNode node = this.objectMapper.convertValue(body, JsonNode.class);
		String repository = node.get("repository").get("full_name").asText();
		log.info("Received a webhook from {}", repository);
		JsonNode commits = node.get("commits").get(0);
		this.update(repository, this.entryIds(commits, "added"));
		this.update(repository, this.entryIds(commits, "modified"));
		this.delete(this.entryIds(commits, "removed"));
		return "OK";
	}

	private List<EntryId> entryIds(JsonNode commits, String type) {
		return stream(commits.get(type).spliterator(), false) //
				.map(JsonNode::asText) //
				.map(s -> s.replace("content/", "")) //
				.filter(Entry::isPublicFileName) //
				.map(EntryId::fromFileName) //
				.collect(Collectors.toList());
	}

	private void update(String repository, List<EntryId> entryIds) {
		entryIds.forEach(entryId -> {
			log.info("Update {}", entryId);
			Entry entry = this.githubClient.get(repository, entryId).block();
			this.entryMapper.save(entry);
		});
	}

	private void delete(List<EntryId> entryIds) {
		entryIds.forEach(entryId -> {
			log.info("Delete {}", entryId);
			this.entryMapper.delete(entryId);
		});
	}

	@PostConstruct
	public void init() {
		if (this.entryMapper == null) {
			this.context = new SpringApplication(BlogUpdaterApplication.class).run();
			this.entryMapper = context.getBean(EntryMapper.class);
		}
	}

	@PreDestroy
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}
}
