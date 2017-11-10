package org.ironrhino.flowable.bpmn.form;

import java.util.Map;

public interface FormRendererHandler {

	public void handle(Map<String, FormElement> formElements,
			String processDefinitionKey, String taskDefinitionKey);

}
