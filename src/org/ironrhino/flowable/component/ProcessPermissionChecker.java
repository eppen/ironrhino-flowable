package org.ironrhino.flowable.component;

import java.util.List;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ProcessPermissionChecker {

	@Autowired
	private TaskService taskService;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private HistoryService historyService;

	public boolean canStart(String processDefinitionKey) {
		List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey(processDefinitionKey).active().orderByProcessDefinitionVersion().desc()
				.listPage(0, 1);
		ProcessDefinition processDefinition = null;
		if (!list.isEmpty())
			processDefinition = list.get(0);
		return canStart(processDefinition);
	}

	protected boolean canStart(ProcessDefinition processDefinition) {
		if (processDefinition == null)
			return false;
		return true;
	}

	public boolean canClaim(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
		return canClaim(task);
	}

	public boolean canClaim(Task task) {
		if (task == null)
			return false;
		HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(task.getProcessInstanceId()).unfinished().singleResult();
		if (hpi == null)
			return false;
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(hpi.getProcessDefinitionId()).singleResult();
		return canClaim(pd.getKey(), task.getTaskDefinitionKey(), hpi.getStartUserId());
	}

	protected boolean canClaim(String processDefinitionKey, String taskDefinitionKey, String startUserId) {
		return true;
	}
}
