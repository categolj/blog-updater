package am.ik.blog.entry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "flyway.enabled=false")
public class EntryUpdaterTest {
	@Autowired
	Sink sink;
	@SpyBean
	EntryGithubClient githubClient;
	@MockBean
	EntryMapper entryMapper;

	@Test
	public void handleAdded() throws Exception {
		Entry entry99999 = Fixtures.entry99999();
		BDDMockito.given(githubClient.get("making/blog.ik.am", entry99999.entryId))
				.willReturn(Mono.just(entry99999));
		Message<?> message = MessageBuilder
				.withPayload(
						"{\"paths\":[\"content/99999.md\"],\"repository\":\"making/blog.ik.am\"}")
				.setHeader("type", "added").build();
		sink.input().send(message);
		Thread.sleep(100);
		ArgumentCaptor<Entry> captor = ArgumentCaptor.forClass(Entry.class);
		Mockito.verify(entryMapper).save(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999);
	}

	@Test
	public void handleModified() throws Exception {
		Entry entry99999 = Fixtures.entry99999();
		BDDMockito.given(githubClient.get("making/blog.ik.am", entry99999.entryId))
				.willReturn(Mono.just(entry99999));
		Message<?> message = MessageBuilder
				.withPayload(
						"{\"paths\":[\"content/99999.md\"],\"repository\":\"making/blog.ik.am\"}")
				.setHeader("type", "modified").build();
		sink.input().send(message);
		Thread.sleep(100);
		ArgumentCaptor<Entry> captor = ArgumentCaptor.forClass(Entry.class);
		Mockito.verify(entryMapper).save(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999);
	}

	@Test
	public void handleRemoved() throws Exception {
		Entry entry99999 = Fixtures.entry99999();
		Message<?> message = MessageBuilder
				.withPayload(
						"{\"paths\":[\"content/99999.md\"],\"repository\":\"making/blog.ik.am\"}")
				.setHeader("type", "removed").build();
		sink.input().send(message);
		Thread.sleep(100);
		ArgumentCaptor<EntryId> captor = ArgumentCaptor.forClass(EntryId.class);
		Mockito.verify(entryMapper).delete(captor.capture());
		assertThat(captor.getValue()).isEqualTo(entry99999.entryId);
	}

}