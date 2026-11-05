package org.uksrc.archive.tap;

import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.javastro.ivoa.entities.uws.ExecutionPhase;
import org.javastro.ivoacore.uws.BaseUWSJob;
import org.javastro.ivoacore.uws.persist.JobStore;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Runs database job-store operations in a JTA transaction, including calls made
 * by background job-monitor threads.
 */
final class TransactionalJobStore implements JobStore {

    private final JobStore delegate;
    private final UserTransaction userTransaction;

    TransactionalJobStore(JobStore delegate, UserTransaction userTransaction) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.userTransaction = Objects.requireNonNull(userTransaction, "userTransaction");
    }

    @Override
    public void store(BaseUWSJob job) {
        inTransaction(() -> {
            delegate.store(job);
            return null;
        });
    }

    @Override
    public BaseUWSJob retrieve(String id) {
        return inTransaction(() -> delegate.retrieve(id));
    }

    @Override
    public boolean delete(String id) {
        return inTransaction(() -> delegate.delete(id));
    }

    @Override
    public Set<String> getAllIds() {
        return inTransaction(delegate::getAllIds);
    }

    @Override
    public List<BaseUWSJob> getJobs(ExecutionPhase phase, ZonedDateTime after, Integer last) {
        return inTransaction(() -> delegate.getJobs(phase, after, last));
    }

    private <T> T inTransaction(Supplier<T> operation) {
        boolean startedHere = false;
        try {
            if (userTransaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
                userTransaction.begin();
                startedHere = true;
            }

            T result = operation.get();
            if (startedHere) {
                userTransaction.commit();
            }
            return result;
        } catch (Exception e) {
            if (startedHere) {
                try {
                    userTransaction.rollback();
                } catch (Exception rollbackException) {
                    e.addSuppressed(rollbackException);
                }
            }
            throw new IllegalStateException("Job-store operation failed", e);
        }
    }
}
