package am.ik.blog.entry;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FrontMatterFactory {
	private final Yaml yaml;

	@SuppressWarnings({ "unchecked" })
	public FrontMatter createFromYaml(String string) {
		Map<String, Object> map = (Map<String, Object>) yaml.load(string);
		Title title = new Title((String) map.getOrDefault("title", "no title"));
		Categories categories = new Categories(
				((List<String>) map.computeIfAbsent("categories", key -> emptyList()))
						.stream().map(Category::new).collect(toList()));
		Tags tags = new Tags(
				((List<String>) map.computeIfAbsent("tags", key -> emptyList())).stream()
						.map(Tag::new).collect(toList()));
		EventTime date = map.containsKey("date")
				? new EventTime(OffsetDateTime.ofInstant(
						((Date) map.get("date")).toInstant(), ZoneId.systemDefault()))
				: EventTime.UNSET;
		EventTime updated = map.containsKey("updated")
				? new EventTime(OffsetDateTime.ofInstant(
						((Date) map.get("updated")).toInstant(), ZoneId.systemDefault()))
				: EventTime.UNSET;
		return new FrontMatter(title, categories, tags, date, updated);
	}
}
