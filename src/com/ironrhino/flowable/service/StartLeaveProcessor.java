package com.ironrhino.flowable.service;

import java.util.Date;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ironrhino.flowable.model.Leave;

@Component
public class StartLeaveProcessor implements ExecutionListener {

	private static final long serialVersionUID = -523387540206288280L;

	@Autowired
	private LeaveManager leaveManager;

	@Autowired
	RuntimeService runtimeService;

	@Autowired
	UserManager userManager;

	@Override
	public void notify(DelegateExecution delegateExecution) {
		Leave leave = new Leave();
		String applyUserId = (String) delegateExecution.getVariable("applyUserId");
		if (applyUserId == null)
			throw new NullPointerException("applyUserId is null");
		User user = (User) userManager.loadUserByUsername(applyUserId);
		leave.setUser(user);
		leave.setNumber(delegateExecution.getProcessInstanceBusinessKey());
		leave.setLeaveType((String) delegateExecution.getVariable("leaveType"));
		leave.setStartTime((Date) delegateExecution.getVariable("startTime"));
		leave.setEndTime((Date) delegateExecution.getVariable("endTime"));
		leave.setReason((String) delegateExecution.getVariable("reason"));
		delegateExecution.setVariable("to", user.getEmail());
		leaveManager.save(leave);

	}

}
