package am.ik.blog.entry;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

public class EntryFactoryTest {
	EntryFactory entryFactory = new EntryFactory();

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void isPublic() throws Exception {
		Resource file = mock(Resource.class);
		given(file.getFilename()).willReturn("00001.md");
		assertThat(entryFactory.isPublic(file)).isTrue();
	}

	@Test
	public void isNotPublic() throws Exception {
		Resource file = mock(Resource.class);
		given(file.getFilename()).willReturn("_00001.md");
		assertThat(entryFactory.isPublic(file)).isFalse();
	}

	@Test
	public void parseEntryId() throws Exception {
		Resource file = mock(Resource.class);
		given(file.getFilename()).willReturn("00001.md");
		assertThat(entryFactory.parseEntryId(file)).isEqualTo(new EntryId(1L));
	}

	@Test
	public void createFromYamlFile() throws Exception {
		Resource file = mock(Resource.class);
		given(file.getInputStream()).willReturn(
				new ByteArrayInputStream(("---\n" + "title: Hello Spring Boot\n"
						+ "tags: [\"Spring\", \"Spring Boot\", \"Java\"]\n"
						+ "categories: [\"Programming\", \"Java\", \"org\", \"springframework\", \"boot\"]\n"
						+ "date: 2015-11-15T23:59:32+09:00\n"
						+ "updated: 2015-11-15T23:59:32+09:00\n" + "---\n" + "\n"
						+ "Content(markdown)\n" + "Here\n" + "\n"
						+ "`date` and `updated` are optional.\n"
						+ "If `date` is not specified, first commit date is used.\n"
						+ "If `updated` is not specified, last commit date is used.")
								.getBytes(StandardCharsets.UTF_8)));
		given(file.getFilename()).willReturn("00001.md");
		Optional<Entry.EntryBuilder> entry = entryFactory.createFromYamlFile(file);
		assertThat(entry.isPresent()).isTrue();
		entry.map(Entry.EntryBuilder::build).ifPresent(e -> {
			assertThat(e.entryId).isEqualTo(new EntryId(1L));
			assertThat(e.content).isEqualTo(new Content("Content(markdown)\n" + "Here\n"
					+ "\n" + "`date` and `updated` are optional.\n"
					+ "If `date` is not specified, first commit date is used.\n"
					+ "If `updated` is not specified, last commit date is used."));
			assertThat(e.frontMatter).isNotNull();
			assertThat(e.frontMatter.title).isEqualTo(new Title("Hello Spring Boot"));
			assertThat(e.frontMatter.tags.collect(toList())).containsExactly(
					new Tag("Spring"), new Tag("Spring Boot"), new Tag("Java"));
			assertThat(e.frontMatter.categories.collect(toList())).containsExactly(
					new Category("Programming"), new Category("Java"),
					new Category("org"), new Category("springframework"),
					new Category("boot"));
		});
	}

	@Test
	public void createFromYamlFileNotPublic() throws Exception {
		Resource file = mock(Resource.class);
		given(file.getFilename()).willReturn("_00001.md");
		Optional<Entry.EntryBuilder> entry = entryFactory.createFromYamlFile(file);
		assertThat(entry.isPresent()).isFalse();
	}
}