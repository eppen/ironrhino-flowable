package org.ironrhino.flowable.bpmn.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricFormProperty;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.RepositoryServiceImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.flowable.bpmn.form.FormRenderer;
import org.ironrhino.flowable.bpmn.model.ActivityDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ProcessTraceService {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private FormRenderer formRenderer;

	@Autowired
	private TaskService taskService;

	@Autowired
	private RepositoryServiceImpl repositoryService;

	@Autowired
	private IdentityService identityService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private ProcessEngineConfigurationImpl processEngineConfiguration;

	public List<ActivityDetail> traceHistoricProcessInstance(String processInstanceId) {
		List<ActivityDetail> details = new ArrayList<ActivityDetail>();
		List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
		for (HistoricActivityInstance activity : activities) {
			if (!"userTask".equals(activity.getActivityType()) && !"startEvent".equals(activity.getActivityType()))
				continue;
			ActivityDetail detail = new ActivityDetail();
			detail.setStartTime(activity.getStartTime());
			detail.setEndTime(activity.getEndTime());
			details.add(detail);
			if ("startEvent".equals(activity.getActivityType())) {
				HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
						.processInstanceId(processInstanceId).singleResult();
				detail.setName("startProcessInstance");
				detail.setAssignee(hpi.getStartUserId());
				List<Attachment> attachments = taskService.getProcessInstanceAttachments(hpi.getId());
				Iterator<Attachment> it = attachments.iterator();
				while (it.hasNext()) {
					Attachment attachment = it.next();
					if (!hpi.getStartUserId().equals(attachment.getUserId()))
						it.remove();
				}
				detail.setAttachments(attachments);

				List<Comment> comments = taskService.getProcessInstanceComments(hpi.getId());
				Iterator<Comment> itc = comments.iterator();
				while (itc.hasNext()) {
					Comment comment = itc.next();
					if (!hpi.getStartUserId().equals(comment.getUserId()))
						itc.remove();
				}
				detail.setComments(comments);
			} else {
				detail.setName(activity.getActivityName());
				detail.setAssignee(activity.getAssignee());
				HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
						.processInstanceId(processInstanceId).taskId(activity.getTaskId()).singleResult();
				List<Attachment> attachments = taskService.getTaskAttachments(task.getId());
				Iterator<Attachment> it = attachments.iterator();
				while (it.hasNext()) {
					Attachment attachment = it.next();
					if (!task.getAssignee().equals(attachment.getUserId()))
						it.remove();
				}
				detail.setAttachments(attachments);
				List<Comment> comments = taskService.getTaskComments(task.getId());
				Iterator<Comment> itc = comments.iterator();
				while (itc.hasNext()) {
					Comment comment = itc.next();
					if (!task.getAssignee().equals(comment.getUserId()))
						itc.remove();
				}
				detail.setComments(comments);
			}
			List<HistoricDetail> list = historyService.createHistoricDetailQuery().activityInstanceId(activity.getId())
					.list();
			for (HistoricDetail hd : list) {
				if (hd instanceof HistoricFormProperty) {
					HistoricFormProperty hfp = (HistoricFormProperty) hd;
					if (hfp.getPropertyValue() != null)
						detail.getData().put(hfp.getPropertyId(), hfp.getPropertyValue());
				}
			}
			detail.setData(formRenderer.display(activity.getProcessDefinitionId(), activity.getActivityId(),
					detail.getData()));

		}
		return details;
	}

	public List<Map<String, Object>> traceProcessDefinition(String processDefinitionId) throws Exception {
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
				.getDeployedProcessDefinition(processDefinitionId);
		BpmnModel bpmnModel = processEngineConfiguration.getDeploymentManager()
				.resolveProcessDefinition(processDefinition).getBpmnModel();
		org.flowable.bpmn.model.Process process = bpmnModel.getProcessById(processDefinition.getKey());
		List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
		for (FlowElement activity : process.getFlowElements()) {
			if (activity instanceof Activity) {
				Map<String, Object> activityImageInfo = packageSingleActivitiInfo(bpmnModel, (Activity) activity, null,
						processDefinition, false);
				activities.add(activityImageInfo);
			}
		}
		return activities;
	}

	public List<Map<String, Object>> traceProcessInstance(String processInstanceId) throws Exception {
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
		List<String> activityIds = new ArrayList<>(executions.size());
		for (Execution execution : executions) {
			if ((execution instanceof ExecutionEntity) && ((ExecutionEntity) execution).isActive())
				activityIds.add(execution.getActivityId());
		}
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
				.getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
		BpmnModel bpmnModel = processEngineConfiguration.getDeploymentManager()
				.resolveProcessDefinition(processDefinition).getBpmnModel();
		org.flowable.bpmn.model.Process process = bpmnModel.getProcessById(processDefinition.getKey());
		List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
		for (FlowElement activity : process.getFlowElements()) {
			if (activity instanceof Activity) {
				boolean current = activityIds.contains(activity.getId());
				Map<String, Object> activityImageInfo = packageSingleActivitiInfo(bpmnModel, (Activity) activity,
						processInstanceId, processDefinition, current);
				activities.add(activityImageInfo);
			}
		}
		return activities;
	}

	private Map<String, Object> packageSingleActivitiInfo(BpmnModel bpmnModel, Activity activity,
			String processInstanceId, ProcessDefinitionEntity processDefinition, boolean current) throws Exception {
		Map<String, Object> activityInfo = new HashMap<String, Object>();
		if (current) {
			activityInfo.put("current", current);
			String key = processDefinition.getKey();
			String diagramResourceName = processDefinition.getDiagramResourceName();
			if (diagramResourceName.equals(key + "." + key + ".png"))
				activityInfo.put("border-radius", 5);
		}
		setPosition(bpmnModel, activity, activityInfo);
		setWidthAndHeight(bpmnModel, activity, activityInfo);
		Map<String, Object> vars = new LinkedHashMap<String, Object>();
		if (processInstanceId != null) {
			List<HistoricActivityInstance> historicActivityInstances = historyService
					.createHistoricActivityInstanceQuery().executionId(processInstanceId).activityId(activity.getId())
					.orderByHistoricActivityInstanceStartTime().desc().list();
			if (!historicActivityInstances.isEmpty()) {
				HistoricActivityInstance hai = historicActivityInstances.get(0);
				if (hai.getAssignee() != null) {
					User assigneeUser = identityService.createUserQuery().userId(hai.getAssignee()).singleResult();
					try {
						UserDetails userDetails = userDetailsService.loadUserByUsername(assigneeUser.getId());
						vars.put(translate("assignee"), userDetails.toString());
					} catch (UsernameNotFoundException e) {
						vars.put(translate("assignee"), assigneeUser.getFirstName());
					}
				}
				if (hai.getStartTime() != null)
					vars.put(translate("startTime"), hai.getStartTime());
				if (hai.getEndTime() != null)
					vars.put(translate("endTime"), hai.getEndTime());
			}
		}
		if (activity instanceof org.flowable.bpmn.model.Task) {
			String type = StringUtils.uncapitalize(activity.getClass().getSimpleName());
			vars.put(translate("taskType"), translate(type));
		}
		if (activity instanceof UserTask) {
			Task currentTask = null;
			if (current) {
				currentTask = getCurrentTaskInfo(processInstanceId, activity.getId());
				if (currentTask != null)
					setCurrentTaskAssignee(vars, currentTask);
			}
			setTaskGroup(vars, (UserTask) activity);

		}

		if (activity.getDocumentation() != null)
			vars.put(translate("documentation"), activity.getDocumentation());

		activityInfo.put("vars", vars);
		return activityInfo;
	}

	private void setTaskGroup(Map<String, Object> vars, UserTask userTask) {
		StringBuilder roles = new StringBuilder();
		for (String group : userTask.getCandidateGroups()) {
			if (group.indexOf("${") < 0) {
				appendRoles(roles, group);
			}
		}
		if (roles.length() > 0) {
			roles.deleteCharAt(roles.length() - 1);
			vars.put(translate("candidateGroup"), roles.toString());
		}
	}

	private void appendRoles(StringBuilder roles, String text) {
		String[] groups = text.split("\\s*,\\s*");
		for (String s : groups) {
			appendRole(roles, s);
		}
	}

	private void appendRole(StringBuilder roles, String role) {
		Group g = identityService.createGroupQuery().groupId(role).singleResult();
		if (g != null) {
			String roleName = g.getName();
			if (roleName == null) {
				roleName = g.getId();
				roleName = translate(roleName);
			}
			roles.append(roleName).append(" ");
		}
	}

	private void setCurrentTaskAssignee(Map<String, Object> vars, Task currentTask) {
		String assignee = currentTask.getAssignee();
		if (assignee != null) {
			User assigneeUser = identityService.createUserQuery().userId(assignee).singleResult();
			try {
				UserDetails userDetails = userDetailsService.loadUserByUsername(assigneeUser.getId());
				vars.put(translate("assignee"), userDetails.toString());
			} catch (UsernameNotFoundException e) {
				vars.put(translate("assignee"), assigneeUser.getFirstName());
			}
		}
	}

	private Task getCurrentTaskInfo(String processInstanceId, String taskName) {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		if (processInstance != null) {
			String activitiId = processInstance.getActivityId();
			return taskService.createTaskQuery().processInstanceId(processInstance.getId())
					.taskDefinitionKey(activitiId).taskName(taskName).singleResult();
		} else {
			return null;
		}
	}

	private void setWidthAndHeight(BpmnModel bpmnModel, Activity activity, Map<String, Object> activityInfo) {
		GraphicInfo gi = bpmnModel.getGraphicInfo(activity.getId());
		if (gi != null) {
			activityInfo.put("width", gi.getWidth());
			activityInfo.put("height", gi.getHeight());
		}
	}

	private void setPosition(BpmnModel bpmnModel, Activity activity, Map<String, Object> activityInfo) {
		GraphicInfo gi = bpmnModel.getGraphicInfo(activity.getId());
		if (gi != null) {
			activityInfo.put("x", gi.getX());
			activityInfo.put("y", gi.getY());
		}
	}

	private String translate(String key) {
		return I18N.getText(key);
	}
}