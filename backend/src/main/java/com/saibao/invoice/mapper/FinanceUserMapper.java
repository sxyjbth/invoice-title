package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.FinanceUser;
import com.saibao.invoice.dto.FinanceAccountPageQueryDTO;
import org.apache.ibatis.annotations.Param;

/** 网页财务端账号持久化接口。 */
public interface FinanceUserMapper {
    long countSuperAdministrators();
    long countFinanceAccounts(FinanceAccountPageQueryDTO query);
    java.util.List<FinanceUser> selectFinanceAccountPage(FinanceAccountPageQueryDTO query);
    FinanceUser selectById(@Param("id") Long id);
    FinanceUser selectByUsername(@Param("username") String username);
    int insert(FinanceUser user);
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash,
                       @Param("updatedBy") Long updatedBy, @Param("changedAt") java.time.LocalDateTime changedAt);
    int updateLastLogin(@Param("id") Long id, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedBy") Long updatedBy, @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
