package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.MaintenanceRecord;
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
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByLorryOrderByMaintenanceDateDesc(Lorry lorry);

    Page<MaintenanceRecord> findByLorryOrderByMaintenanceDateDesc(Lorry lorry, Pageable pageable);

    List<MaintenanceRecord> findByLorryAndOwnerOrderByMaintenanceDateDesc(Lorry lorry, User owner);

    List<MaintenanceRecord> findByOwnerOrderByMaintenanceDateDesc(User owner);

    List<MaintenanceRecord> findByOwnerAndMaintenanceDateBetweenOrderByMaintenanceDateDesc(User owner, LocalDate startDate, LocalDate endDate);

    long countByLorry(Lorry lorry);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.lorry = :lorry")
    BigDecimal sumCostByLorry(@Param("lorry") Lorry lorry);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.owner = :owner")
    BigDecimal sumCostByOwner(@Param("owner") User owner);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m " +
            "WHERE m.owner = :owner AND m.maintenanceDate >= :startDate AND m.maintenanceDate <= :endDate")
    BigDecimal sumCostByOwnerAndDateRange(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(m) FROM MaintenanceRecord m " +
            "WHERE m.owner = :owner AND m.maintenanceDate >= :startDate AND m.maintenanceDate <= :endDate")
    long countByOwnerAndDateRange(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT m.lorry.id, COUNT(m.id), COALESCE(SUM(m.cost), 0), MAX(m.maintenanceDate) " +
            "FROM MaintenanceRecord m WHERE m.owner = :owner GROUP BY m.lorry.id")
    List<Object[]> findLorryMaintenanceAggregatesByOwner(@Param("owner") User owner);

    @Query("SELECT m.lorry.id, m.nextServiceDate FROM MaintenanceRecord m " +
            "WHERE m.owner = :owner AND m.nextServiceDate IS NOT NULL " +
            "ORDER BY m.maintenanceDate DESC")
    List<Object[]> findLatestNextServiceDatesByOwner(@Param("owner") User owner);

    @Query("SELECT MONTH(m.maintenanceDate), COALESCE(SUM(m.cost), 0) " +
            "FROM MaintenanceRecord m WHERE m.owner = :owner " +
            "AND m.maintenanceDate >= :startDate AND m.maintenanceDate <= :endDate " +
            "GROUP BY MONTH(m.maintenanceDate)")
    List<Object[]> sumCostByOwnerGroupedByMonth(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT DISTINCT YEAR(m.maintenanceDate) FROM MaintenanceRecord m " +
            "WHERE m.owner = :owner ORDER BY YEAR(m.maintenanceDate) DESC")
    List<Integer> findDistinctYearsByOwner(@Param("owner") User owner);
}
