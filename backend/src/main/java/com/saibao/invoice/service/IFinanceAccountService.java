package com.saibao.invoice.service;

import com.saibao.invoice.dto.CreateFinanceAccountDTO;
import com.saibao.invoice.dto.FinanceAccountPageQueryDTO;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.PageResult;

/** 网页财务端账号及密码服务。 */
public interface IFinanceAccountService {
    FinanceAccountVO createFinanceAccount(Long operatorId, CreateFinanceAccountDTO request);
    PageResult<FinanceAccountVO> pageFinanceAccounts(FinanceAccountPageQueryDTO query);
    FinanceAccountVO authenticate(String username, String rawPassword);
    FinanceAccountVO getById(Long id);
    void changeOwnPassword(Long accountId, String currentPassword, String newPassword);
    void resetPassword(Long operatorId, Long accountId, String newPassword);
    void updateStatus(Long operatorId, Long accountId, String status);
}
