package org.ironrhino.flowable.bpmn.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.ironrhino.flowable.bpmn.component.ProcessPermissionChecker;
import org.ironrhino.flowable.bpmn.model.TaskQueryCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessHelper {

	@Autowired
	protected RepositoryService repositoryService;

	@Autowired
	private TaskService taskService;

	@Autowired(required = false)
	private ProcessPermissionChecker processPermissionChecker;

	public Map<String, String> findStartableProcessMap(String userId) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		List<ProcessDefinition> processDefinitions = repositoryService
				.createProcessDefinitionQuery().active()
				.startableByUser(userId).orderByProcessDefinitionKey().asc()
				.orderByProcessDefinitionVersion().desc().list();
		for (ProcessDefinition processDefinition : processDefinitions)
			if (!map.containsKey(processDefinition.getKey()))
				map.put(processDefinition.getKey(), processDefinition.getName());
		return map;
	}

	public List<Task> findAssignedTasks(String userId) {
		return findAssignedTasks(userId, null);
	}

	public List<Task> findAssignedTasks(String userId,
			TaskQueryCriteria criteria) {
		TaskQuery query = taskService.createTaskQuery().active()
				.taskAssignee(userId);
		if (criteria != null)
			criteria.filter(query, true);
		return query.orderByTaskPriority().desc().orderByTaskCreateTime()
				.desc().list();
	}

	public long countAssignedTasks(String userId) {
		return countAssignedTasks(userId, null);
	}

	public long countAssignedTasks(String userId, TaskQueryCriteria criteria) {
		TaskQuery query = taskService.createTaskQuery().active()
				.taskAssignee(userId);
		if (criteria != null)
			criteria.filter(query, true);
		return query.count();
	}

	public List<Task> findCandidateTasks(String userId) {
		return findCandidateTasks(userId, null);
	}

	public List<Task> findCandidateTasks(String userId,
			TaskQueryCriteria criteria) {
		TaskQuery query = taskService.createTaskQuery().active()
				.taskCandidateUser(userId);
		if (criteria != null)
			criteria.filter(query, true);
		List<Task> taskCandidates = query.orderByTaskPriority().desc()
				.orderByTaskCreateTime().desc().list();
		if (processPermissionChecker == null)
			return taskCandidates;
		List<Task> list = new ArrayList<Task>();
		for (Task task : taskCandidates) {
			if (!processPermissionChecker.canClaim(task))
				continue;
			list.add(task);
		}
		return list;
	}

	public long countCandidateTasks(String userId) {
		return countCandidateTasks(userId, null);
	}

	public long countCandidateTasks(String userId, TaskQueryCriteria criteria) {
		if (processPermissionChecker == null) {
			TaskQuery query = taskService.createTaskQuery().active()
					.taskCandidateUser(userId);
			if (criteria != null)
				criteria.filter(query, true);
			return query.count();
		} else
			return findCandidateTasks(userId, criteria).size();
	}

}