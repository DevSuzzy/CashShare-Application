package com.bctech.cashshareapplication.persistence.repository;

import com.bctech.cashshareapplication.model.enums.RoleEnum;
import com.bctech.cashshareapplication.persistence.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {

Role findRoleByName(RoleEnum roleEnum);
}
