package com.lorryowner.backend.repository;

import com.lorryowner.backend.entity.Lorry;
import com.lorryowner.backend.entity.MaintenanceRecord;
import com.lorryowner.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByLorryOrderByMaintenanceDateDesc(Lorry lorry);

    List<MaintenanceRecord> findByLorryAndOwnerOrderByMaintenanceDateDesc(Lorry lorry, User owner);

    List<MaintenanceRecord> findByOwnerOrderByMaintenanceDateDesc(User owner);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.lorry = :lorry")
    BigDecimal sumCostByLorry(@Param("lorry") Lorry lorry);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m WHERE m.owner = :owner")
    BigDecimal sumCostByOwner(@Param("owner") User owner);
}
