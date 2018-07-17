package org.ironrhino.flowable.bpmn.action;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.identitylink.api.IdentityLink;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.flowable.bpmn.model.ProcessInstanceQueryCriteria;
import org.ironrhino.flowable.bpmn.model.Row;
import org.ironrhino.flowable.bpmn.service.ProcessTraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
public class ProcessInstanceAction extends BaseAction {

	private static final long serialVersionUID = -6657349245825745444L;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private ProcessTraceService processTraceService;

	@Getter
	@Setter
	private ResultPage<Row> resultPage;

	@Getter
	private ProcessInstance processInstance;

	@Getter
	@Setter
	private ProcessInstanceQueryCriteria criteria;

	@Getter
	private List<Map<String, Object>> activities;

	@Override
	public String execute() {
		return list();
	}

	public String list() {
		if (resultPage == null)
			resultPage = new ResultPage<Row>();
		ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
		boolean admin = AuthzUtils.authorize(null, UserRole.ROLE_ADMINISTRATOR, null);
		if (criteria != null)
			criteria.filter(query, admin);
		if (!admin)
			query.involvedUser(AuthzUtils.getUsername());
		String processDefinitionId = getUid();
		if (StringUtils.isNotBlank(processDefinitionId))
			query = query.processDefinitionId(processDefinitionId);
		long count = query.count();
		resultPage.setTotalResults(count);
		if (count > 0) {
			List<ProcessInstance> processInstances = query.orderByProcessInstanceId().desc()
					.listPage(resultPage.getStart(), resultPage.getPageSize());
			List<Row> list = new ArrayList<Row>(processInstances.size());
			for (ProcessInstance pi : processInstances) {
				Row row = new Row();
				row.setId(pi.getId());
				row.setProcessInstance(pi);
				row.setHistoricProcessInstance(historyService.createHistoricProcessInstanceQuery()
						.processInstanceId(pi.getProcessInstanceId()).singleResult());
				row.setProcessDefinition(repositoryService.createProcessDefinitionQuery()
						.processDefinitionId(pi.getProcessDefinitionId()).singleResult());
				List<HistoricActivityInstance> historicActivityInstances = historyService
						.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
						.orderByHistoricActivityInstanceStartTime().desc().list();
				if (!historicActivityInstances.isEmpty()) {
					for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
						if (historicActivityInstance.getActivityId() != null
								&& historicActivityInstance.getEndTime() == null) {
							row.setHistoricActivityInstance(historicActivityInstance);
							break;
						}
					}
				}
				list.add(row);
			}
			resultPage.setResult(list);
		}
		return LIST;
	}

	@Override
	public String view() {
		processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(getUid()).singleResult();
		if (processInstance == null)
			processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(getUid())
					.singleResult();
		if (processInstance == null)
			return NOTFOUND;
		if (!canView(processInstance.getId()))
			return ACCESSDENIED;
		return VIEW;
	}

	public String diagram() throws Exception {
		if (!canView(getUid()))
			return NOTFOUND;
		InputStream resourceAsStream = null;
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(getUid()).singleResult();
		if (processInstance == null)
			processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(getUid())
					.singleResult();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
		String resourceName = processDefinition.getDiagramResourceName();
		resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
		byte[] byteArray = StreamUtils.copyToByteArray(resourceAsStream);
		ServletOutputStream servletOutputStream = ServletActionContext.getResponse().getOutputStream();
		servletOutputStream.write(byteArray, 0, byteArray.length);
		servletOutputStream.flush();
		servletOutputStream.close();
		return NONE;
	}

	@JsonConfig(root = "activities")
	public String trace() throws Exception {
		if (!canView(getUid()))
			return NOTFOUND;
		activities = processTraceService.traceProcessInstance(getUid());
		return JSON;
	}

	@Override
	@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
	public String delete() {
		if (getId() != null) {
			Set<String> ids = new LinkedHashSet<>();
			for (String id : getId()) {
				ids.add(id);
			}
			List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().processInstanceIds(ids).list();
			for (ProcessInstance pi : list) {
				if (!(pi.isSuspended() || pi.isEnded())) {
					addActionError("流程" + pi.getId() + "必须先挂起才能删除");
					return ERROR;
				}
			}
			for (ProcessInstance pi : list)
				runtimeService.deleteProcessInstance(pi.getId(), "delete by administrator");
			addActionMessage(getText("delete.success"));
		}
		return SUCCESS;
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
	public String suspend() throws Exception {
		if (getId() != null) {
			for (String id : getId())
				runtimeService.suspendProcessInstanceById(id);
			addActionMessage(getText("operate.success"));
		}
		return SUCCESS;
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
	public String activate() throws Exception {
		if (getId() != null) {
			for (String id : getId())
				runtimeService.activateProcessInstanceById(id);
			addActionMessage(getText("operate.success"));
		}
		return SUCCESS;
	}

	private boolean canView(String processInstanceId) {
		if (AuthzUtils.authorize(null, UserRole.ROLE_ADMINISTRATOR, null))
			return true;
		String userId = AuthzUtils.getUsername();
		List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
		for (IdentityLink identityLink : identityLinks)
			if (userId.equals(identityLink.getUserId()))
				return true;
		return false;
	}
}
