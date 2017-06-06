package am.ik.blog.entry;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

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
}