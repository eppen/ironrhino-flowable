package com.ironrhino.flowable.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Owner;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.security.model.User;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "`leave`")
@AutoConfig
@Owner(propertyName = "user")
@Authorize(ifAnyGranted = UserRole.employee)
@Richtable(readonly = @Readonly(true), bottomButtons = "<a class='btn noid' href='<@url value='/bpmn/task/form?processDefinitionKey=leave'/>' rel='richtable'>请假</a> <button type='button' class='btn reload'>${action.getText('reload')}</button> <button type='button' class='btn filter'>${action.getText('filter')}</button>", order = "applyTime desc")
@Getter
@Setter
public class Leave extends BaseEntity {

	private static final long serialVersionUID = -3509600479976901201L;

	@ManyToOne(optional = false)
	@JoinColumn(updatable = false)
	@UiConfig(displayOrder = 1, width = "100px", alias = "申请人", hiddenInList = @Hidden(expression = "!Parameters.user??"), hiddenInView = @Hidden(expression = "!Parameters.user??"))
	private User user;

	@NaturalId
	@UiConfig(displayOrder = 2, width = "100px", alias = "编号", template = "<#if entity.realityStartTime??>${value}<#else><a href=\"<@url value='/bpmn/processInstance/view/${value}'/>\" target=\"_blank\">${value}</a></#if>")
	private String number;

	@UiConfig(displayOrder = 4, width = "80px")
	@Temporal(TemporalType.DATE)
	private Date startTime;

	@UiConfig(displayOrder = 5, width = "80px")
	@Temporal(TemporalType.DATE)
	private Date endTime;

	@UiConfig(displayOrder = 6, width = "100px")
	@Temporal(TemporalType.DATE)
	private Date realityStartTime;

	@UiConfig(displayOrder = 7, width = "100px")
	@Temporal(TemporalType.DATE)
	private Date realityEndTime;

	@UiConfig(displayOrder = 8, width = "130px")
	private Date applyTime = new Date();

	@UiConfig(displayOrder = 9, width = "80px", type = "dictionary", templateName = "leaveType")
	private String leaveType;

	@UiConfig(displayOrder = 10, type = "textarea")
	@Column(length = 4000)
	private String reason;

}
