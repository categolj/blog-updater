package am.ik.blog.entry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Fixtures {
	public static Entry entry99999() {
		return Entry.builder().entryId(new EntryId(99999L))
				.created(new Author(new Name("test"), EventTime.UNSET))
				.updated(new Author(new Name("test"), EventTime.UNSET))
				.frontMatter(new FrontMatter(new Title("test"),
						new Categories(Arrays.asList(new Category("category"))),
						new Tags(Arrays.asList(new Tag("tag"))),
						new EventTime(OffsetDateTime.of(2017, 4, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new EventTime(OffsetDateTime.of(2017, 5, 1, 1, 0, 0, 0,
								ZoneOffset.ofHours(9))),
						new PremiumPoint(100)))
				.content(new Content("test data!")).build().useFrontMatterDate();
	}

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static WebHook added() {
		try {
			return new WebHook(objectMapper.readValue(
					new ClassPathResource("added-payload.json").getInputStream(),
					new TypeReference<Map<String, Object>>() {
					}));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static WebHook modified() {
		try {
			return new WebHook(objectMapper.readValue(
					new ClassPathResource("modified-payload.json").getInputStream(),
					new TypeReference<Map<String, Object>>() {
					}));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static WebHook removed() {
		try {
			return new WebHook(objectMapper.readValue(
					new ClassPathResource("removed-payload.json").getInputStream(),
					new TypeReference<Map<String, Object>>() {
					}));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static class WebHook {
		private final Map<String, Object> payload;

		public WebHook(Map<String, Object> payload) {
			this.payload = payload;
		}

		public Map<String, Object> payload() {
			return payload;
		}
	}
}