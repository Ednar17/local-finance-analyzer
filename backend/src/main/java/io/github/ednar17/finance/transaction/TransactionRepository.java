package io.github.ednar17.finance.transaction;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOperationDateBetweenOrderByOperationDateDesc(LocalDate from, LocalDate to);

    @Query("select t.dedupKey from Transaction t where t.dedupKey in :keys")
    Set<String> findExistingKeys(@Param("keys") Collection<String> keys);
}
