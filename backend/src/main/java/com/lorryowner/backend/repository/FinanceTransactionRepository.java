package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.FinanceTransaction;
import com.lorryowner.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    Page<FinanceTransaction> findByOwnerAndTransactionDateBetweenOrderByTransactionDateDesc(
            User owner,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<FinanceTransaction> findByOwnerAndTransactionDateBetweenOrderByTransactionDateDesc(
            User owner,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft " +
            "WHERE ft.owner = :owner AND ft.transactionType = 'INCOME' AND ft.paymentStatus = 'RECEIVED' " +
            "AND ft.transactionDate >= :startDate AND ft.transactionDate <= :endDate")
    BigDecimal sumIncomeByOwnerAndDateRange(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(ft.amount), 0) FROM FinanceTransaction ft " +
            "WHERE ft.owner = :owner AND ft.transactionType = 'EXPENSE' " +
            "AND ft.transactionDate >= :startDate AND ft.transactionDate <= :endDate")
    BigDecimal sumExpenseByOwnerAndDateRange(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT FUNCTION('MONTH', ft.transactionDate), ft.transactionType, COALESCE(SUM(ft.amount), 0) " +
            "FROM FinanceTransaction ft WHERE ft.owner = :owner " +
            "AND ft.transactionDate >= :startDate AND ft.transactionDate <= :endDate " +
            "AND (ft.transactionType = 'EXPENSE' OR (ft.transactionType = 'INCOME' AND ft.paymentStatus = 'RECEIVED')) " +
            "GROUP BY FUNCTION('MONTH', ft.transactionDate), ft.transactionType")
    List<Object[]> sumByOwnerGroupedByMonthAndType(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ft.category, ft.transactionType, COALESCE(SUM(ft.amount), 0) " +
            "FROM FinanceTransaction ft WHERE ft.owner = :owner " +
            "AND ft.transactionDate >= :startDate AND ft.transactionDate <= :endDate " +
            "AND (ft.transactionType = 'EXPENSE' OR (ft.transactionType = 'INCOME' AND ft.paymentStatus = 'RECEIVED')) " +
            "GROUP BY ft.category, ft.transactionType")
    List<Object[]> sumByOwnerGroupedByCategory(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT FUNCTION('YEAR', ft.transactionDate) FROM FinanceTransaction ft " +
            "WHERE ft.owner = :owner ORDER BY FUNCTION('YEAR', ft.transactionDate) DESC")
    List<Integer> findDistinctYearsByOwner(@Param("owner") User owner);
}
