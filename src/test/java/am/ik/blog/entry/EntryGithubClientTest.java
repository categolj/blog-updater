package am.ik.blog.entry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EntryGithubClientTest {
	EntryGithubClient githubClient = new EntryGithubClient(new ObjectMapper());

	@Test
	public void test() {
		EntryId entryId = new EntryId(412L);
		Entry entry = githubClient.get("making/blog.ik.am", entryId)
				.doOnSuccess(System.out::println).block();
		System.out.println(entry);
		assertThat(entry).isNotNull();
		assertThat(entry.entryId).isEqualTo(entryId);
		assertThat(entry.content).isNotNull();
		assertThat(entry.frontMatter).isNotNull();
		assertThat(entry.created).isNotNull();
		assertThat(entry.updated).isNotNull();
	}
}