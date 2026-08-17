package com.saibao.invoice.service;

/** 为每个浏览器页签签发、校验和撤销独立的财务端登录会话。 */
public interface IFinanceSessionTokenService {
    String create(Long financeUserId);

    Long resolveAccountId(String rawToken);

    void invalidate(String rawToken);
}
