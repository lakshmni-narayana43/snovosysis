package com.ledgerline.repository;

import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    Page<LedgerEntry> findByAccountId(Long accountId, Pageable pageable);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT COALESCE(SUM(le.amount), 0.0000) FROM LedgerEntry le WHERE le.entryType = :entryType")
    BigDecimal sumTotalByEntryType(@Param("entryType") EntryType entryType);

    @Query("SELECT COALESCE(SUM(le.amount), 0.0000) FROM LedgerEntry le WHERE le.account.id = :accountId AND le.entryType = :entryType")
    BigDecimal sumTotalByAccountAndEntryType(@Param("accountId") Long accountId, @Param("entryType") EntryType entryType);

    @Query("SELECT le.account.id, le.entryType, SUM(le.amount) FROM LedgerEntry le GROUP BY le.account.id, le.entryType")
    List<Object[]> sumTotalsPerAccount();
}

