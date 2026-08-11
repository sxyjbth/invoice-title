package com.saibao.invoice.controller;

import com.saibao.invoice.config.EmployeeSessionAuthenticationFilter;
import com.saibao.invoice.dto.DingTalkLoginDTO;
import com.saibao.invoice.service.IEmployeeAuthService;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingTalkOrganizationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 员工端钉钉工作台免登会话接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employee/auth")
@Tag(name = "员工端-钉钉登录", description = "使用钉钉一次性免登码建立员工服务端会话")
public class EmployeeAuthController {
    private final IEmployeeAuthService employeeAuthService;

    @PostMapping("/dingtalk")
    @Operation(summary = "钉钉免登", description = "服务端向钉钉校验一次性 authCode，并仅允许已同步且在职的员工登录")
    public DingEmployeeVO login(@Valid @RequestBody DingTalkLoginDTO request, HttpServletRequest servletRequest) {
        DingEmployeeVO employee = employeeAuthService.authenticate(request.getCorpCode(), request.getAuthCode());
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            session = servletRequest.getSession(true);
        } else {
            // 认证成功后轮换会话 ID，避免攻击者复用认证前的固定会话。
            servletRequest.changeSessionId();
        }
        session.setAttribute(EmployeeSessionAuthenticationFilter.SESSION_EMPLOYEE_ID, employee.getId());
        return employee;
    }

    @GetMapping("/organizations")
    @Operation(summary = "查询已接入钉钉企业", description = "只返回 corpCode、企业名称和 corpId，不返回任何应用密钥")
    public List<DingTalkOrganizationVO> organizations() {
        return employeeAuthService.listOrganizations();
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录员工")
    public DingEmployeeVO me(@AuthenticationPrincipal DingEmployeeVO employee) {
        return employee;
    }

    @PostMapping("/logout")
    @Operation(summary = "退出员工端")
    public void logout(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
