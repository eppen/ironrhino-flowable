package com.ironrhino.flowable.service;

import java.util.Date;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ironrhino.flowable.model.Leave;

@Component
public class AfterModifyApplyContentProcessor implements TaskListener {

	private static final long serialVersionUID = -523387540206288280L;

	@Autowired
	private LeaveManager leaveManager;

	@Autowired
	RuntimeService runtimeService;

	@Override
	public void notify(DelegateTask delegateTask) {
		ProcessInstance processInstance = runtimeService
				.createProcessInstanceQuery()
				.processInstanceId(delegateTask.getProcessInstanceId())
				.singleResult();
		Leave leave = leaveManager.findOne(processInstance.getBusinessKey());
		leave.setStartTime((Date) delegateTask.getVariable("startTime"));
		leave.setEndTime((Date) delegateTask.getVariable("endTime"));
		leaveManager.save(leave);
	}

}
