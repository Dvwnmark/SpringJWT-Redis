package com.bkz.dvn.repository;

import com.bkz.dvn.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @Query(value = "SELECT permission FROM ROLE_PERMISSION WHERE role_id IN (:roleIds)", nativeQuery = true)
    List<String> permissions(@Param("roleIds") List<Long> roleIds);
}
