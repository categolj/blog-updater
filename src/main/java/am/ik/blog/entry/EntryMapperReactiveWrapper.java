package am.ik.blog.entry;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
public class EntryMapperReactiveWrapper {
	private final EntryMapper entryMapper;
	private final TransactionTemplate tx;

	public EntryMapperReactiveWrapper(EntryMapper entryMapper,
			PlatformTransactionManager transactionManager) {
		this.entryMapper = entryMapper;
		this.tx = new TransactionTemplate(transactionManager);
	}

	public Mono<Entry> findOne(EntryId entryId) {
		return Mono.defer(() -> Mono.justOrEmpty(entryMapper.findOne(entryId, false)))
				.subscribeOn(Schedulers.elastic());
	}

	public Mono<Void> save(Mono<Entry> entry) {
		return entry //
				.publishOn(Schedulers.elastic()) //
				.doOnSuccess(e -> tx.execute(s -> {
					// TODO
					entryMapper.save(e);
					return null;
				})) //
				.then();
	}

	public Mono<Void> delete(EntryId entryId) {
		return Mono.just(entryId) //
				.publishOn(Schedulers.elastic()) //
				.doOnSuccess(id -> tx.execute(s -> {
					// TODO
					entryMapper.delete(entryId);
					return null;
				})) //
				.then();
	}
}
