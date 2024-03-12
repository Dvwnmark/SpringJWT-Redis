package com.bkz.dvn.entity;
import lombok.Data;

import javax.persistence.*;
@Data
@Entity
@Table(name = "ROLE_PERMISSION")
public class RolePermission {
    @Id
    @Column(name = "permissionId")
    private Long permissionId;

    @Column(name = "permission")
    private String permission;

    @ManyToOne
    @JoinColumn(name = "roleId")
    private Role role;

}
