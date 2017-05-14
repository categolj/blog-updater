package am.ik.blog.entry;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class WebHookRequest {
	private List<String> paths;

	public List<EntryId> getEntryIds() {
		return paths.stream().map(s -> s.replace("content/", ""))
				.filter(Entry::isPublicFileName).map(EntryId::fromFileName)
				.collect(Collectors.toList());
	}
}