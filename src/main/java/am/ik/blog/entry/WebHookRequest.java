package am.ik.blog.entry;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class WebHookRequest {
	private List<String> paths;
	private String repository;

	public List<EntryId> getEntryIds() {
		return paths.stream().map(s -> s.replace("content/", ""))
				.filter(EntryFactory::isPublicFileName).map(EntryFactory::parseEntryId)
				.collect(Collectors.toList());
	}
}