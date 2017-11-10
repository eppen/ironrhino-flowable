package org.ironrhino.flowable.bpmn.model;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Row {

	private String id;

	private Deployment deployment;

	private ProcessDefinition processDefinition;

	private ProcessInstance processInstance;

	private HistoricProcessInstance historicProcessInstance;

	private HistoricActivityInstance historicActivityInstance;

	private Task task;

}
