package am.ik.blog.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "flyway.enabled=false")
public class EntryUpdaterTest {
	@SpyBean
	EntryGithubClient githubClient;
	@MockBean
	EntryMapper entryMapper;

	EntryUpdater updater = new EntryUpdater();

	@Before
	public void setup() {
		updater.entryMapper = entryMapper;
		updater.githubClient = githubClient;
	}

	@Test
	public void handleAdded() throws Exception {
		Entry entry99999 = Fixtures.entry99999();
		given(githubClient.get("making/blog.ik.am", entry99999.entryId))
				.willReturn(Mono.just(entry99999));

		Map<String, Object> req = Fixtures.added().payload();

		String res = updater.apply(req);
		assertThat(res).isEqualTo("OK");

		ArgumentCaptor<Entry> captor = ArgumentCaptor.forClass(Entry.class);
		verify(entryMapper).save(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999);
	}

	@Test
	public void handleModified() throws Exception {
		Entry entry99999 = Fixtures.entry99999();
		given(githubClient.get("making/blog.ik.am", entry99999.entryId))
				.willReturn(Mono.just(entry99999));

		Map<String, Object> req = Fixtures.modified().payload();

		String res = updater.apply(req);
		assertThat(res).isEqualTo("OK");

		ArgumentCaptor<Entry> captor = ArgumentCaptor.forClass(Entry.class);
		verify(entryMapper).save(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999);
	}

	@Test
	public void handleRemoved() throws Exception {
		Entry entry99999 = Fixtures.entry99999();

		Map<String, Object> req = Fixtures.removed().payload();

		String res = updater.apply(req);
		assertThat(res).isEqualTo("OK");

		ArgumentCaptor<EntryId> captor = ArgumentCaptor.forClass(EntryId.class);
		verify(entryMapper).delete(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999.entryId);
	}

}