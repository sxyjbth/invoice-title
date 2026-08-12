package com.saibao.invoice.domain;

/** 已发布抬头占用主体的查询结果，用于阻止一个主体同时展示多个有效抬头。 */
public record PublishedSubjectBinding(
        Long subjectId,
        String subjectName,
        Long titleId,
        String companyName) {
}
