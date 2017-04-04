package am.ik.blog.entry;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class FrontMatterFactoryTest {
	FrontMatterFactory frontMatterFactory = new FrontMatterFactory(new Yaml());

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void createFromYaml() throws Exception {
		FrontMatter frontMatter = frontMatterFactory
				.createFromYaml("title: Cloud FoundryのUAAでConcourseのチーム機能を使う\n"
						+ "tags: [\"Concourse CI\", \"Cloud Foundry\"]\n"
						+ "categories: [\"Dev\", \"CI\", \"ConcourseCI\"]");
		assertThat(frontMatter.title)
				.isEqualTo(new Title("Cloud FoundryのUAAでConcourseのチーム機能を使う"));
		assertThat(frontMatter.tags.size()).isEqualTo(2);
		assertThat(frontMatter.tags.collect(toList()))
				.containsExactly(new Tag("Concourse CI"), new Tag("Cloud Foundry"));
		assertThat(frontMatter.categories.size()).isEqualTo(3);
		assertThat(frontMatter.categories.collect(toList())).containsExactly(
				new Category("Dev"), new Category("CI"), new Category("ConcourseCI"));
		assertThat(frontMatter.date).isEqualTo(EventTime.UNSET);
		assertThat(frontMatter.updated).isEqualTo(EventTime.UNSET);
	}

	@Test
	public void createFromYamlWithDate() throws Exception {
		FrontMatter frontMatter = frontMatterFactory
				.createFromYaml("title: Cloud FoundryのUAAでConcourseのチーム機能を使う\n"
						+ "tags: [\"Concourse CI\", \"Cloud Foundry\"]\n"
						+ "categories: [\"Dev\", \"CI\", \"ConcourseCI\"]\n"
						+ "date: 2017-04-11T02:22:32+09:00\n"
						+ "updated: 2017-04-12T02:22:32+09:00");
		assertThat(frontMatter.title)
				.isEqualTo(new Title("Cloud FoundryのUAAでConcourseのチーム機能を使う"));
		assertThat(frontMatter.tags.size()).isEqualTo(2);
		assertThat(frontMatter.tags.collect(toList()))
				.containsExactly(new Tag("Concourse CI"), new Tag("Cloud Foundry"));
		assertThat(frontMatter.categories.size()).isEqualTo(3);
		assertThat(frontMatter.categories.collect(toList())).containsExactly(
				new Category("Dev"), new Category("CI"), new Category("ConcourseCI"));
		assertThat(frontMatter.date).isEqualTo(new EventTime(
				OffsetDateTime.of(2017, 4, 11, 2, 22, 32, 0, ZoneOffset.ofHours(9))));
		assertThat(frontMatter.updated).isEqualTo(new EventTime(
				OffsetDateTime.of(2017, 4, 12, 2, 22, 32, 0, ZoneOffset.ofHours(9))));
	}
}