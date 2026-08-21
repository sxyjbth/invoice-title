package com.saibao.invoice.service;

import com.saibao.invoice.dto.SubjectPermissionPageQueryDTO;
import com.saibao.invoice.dto.SubjectPermissionSaveDTO;
import com.saibao.invoice.dto.SubjectPermissionProfileSaveDTO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.SubjectPermissionVO;
import com.saibao.invoice.vo.SubjectPermissionProfileVO;

/** 主体查看权限业务服务。 */
public interface ISubjectPermissionService {
    PageResult<SubjectPermissionVO> page(SubjectPermissionPageQueryDTO query);
    Long create(SubjectPermissionSaveDTO request);
    void changeStatus(Long id, String status, String operatorUserId);
    SubjectPermissionProfileVO getProfile(Long subjectId);
    SubjectPermissionProfileVO saveProfile(Long subjectId, SubjectPermissionProfileSaveDTO request, String operatorUserId);
    SubjectPermissionProfileVO updateAllEmployeeVisible(Long subjectId,
                                                        boolean allEmployeeVisible,
                                                        String operatorUserId);
}
