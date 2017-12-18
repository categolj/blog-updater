package am.ik.blog.entry;

import org.springframework.stereotype.Repository;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@AllArgsConstructor
public class EntryMapperReactiveWrapper {
	private final EntryMapper entryMapper;

	public Mono<Entry> findOne(EntryId entryId) {
		return Mono.defer(() -> Mono.justOrEmpty(entryMapper.findOne(entryId, false)))
				.subscribeOn(Schedulers.elastic());
	}

	public Mono<Void> save(Mono<Entry> entry) {
		return entry.publishOn(Schedulers.elastic()).doOnSuccess(entryMapper::save)
				.then();
	}

	public Mono<Void> delete(EntryId entryId) {
		return Mono.just(entryId).publishOn(Schedulers.elastic())
				.doOnSuccess(entryMapper::delete).then();
	}
}
