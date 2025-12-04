package com.kis.wmsapplication.modules.userModule.repository;

import com.kis.wmsapplication.modules.userModule.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kis.wmsapplication.modules.userModule.model.UserAuthInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserAuthInfoRepository extends JpaRepository<UserAuthInfo, UUID> {

    Optional<UserAuthInfo> findByEmail(String email);
    Optional<UserAuthInfo> findByPhoneNumber(String phoneNumber);


}
