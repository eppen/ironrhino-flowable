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
public class ReportBackEndProcessor implements TaskListener {

	private static final long serialVersionUID = 3124655854713421538L;

	@Autowired
	private LeaveManager leaveManager;

	@Autowired
	RuntimeService runtimeService;

	@Override
	public void notify(DelegateTask delegateTask) {
		String processInstanceId = delegateTask.getProcessInstanceId();
		ProcessInstance processInstance = runtimeService
				.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		Leave leave = leaveManager.findOne(processInstance.getBusinessKey());
		leave.setRealityStartTime((Date) delegateTask.getVariable("realityStartTime"));
		leave.setRealityEndTime((Date)  delegateTask.getVariable("realityEndTime"));
		leaveManager.save(leave);
	}

}
