package org.ironrhino.flowable.spring;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.form.AbstractFormType;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.variable.api.types.VariableType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

public class SpringProcessEngineConfiguration extends org.flowable.spring.SpringProcessEngineConfiguration {

	@Autowired(required = false)
	private List<AbstractFormType> formTypeList;

	@Autowired(required = false)
	private List<VariableType> variableTypeList;

	@Autowired(required = false)
	private List<FlowableEventListener> listeners;

	public ProcessEngineConfiguration setAnnotationFontName(String annotationFontName) {
		// hack
		try {
			ProcessEngineConfiguration.class.getDeclaredField("annotationFontName");
			this.annotationFontName = annotationFontName;
		} catch (NoSuchFieldException e) {

		}
		return this;
	}

	@Override
	public void initFormTypes() {
		super.initFormTypes();
		if (formTypeList != null)
			for (AbstractFormType customFormType : formTypeList)
				formTypes.addFormType(customFormType);
	}

	@Override
	public void initVariableTypes() {
		super.initVariableTypes();
		if (variableTypeList != null)
			for (VariableType customVariableType : variableTypeList)
				variableTypes.addType(customVariableType, 1);
	}

	@Override
	public void initEventDispatcher() {
		super.initEventDispatcher();
		if (listeners != null)
			for (FlowableEventListener listener : listeners)
				this.eventDispatcher.addEventListener(listener);
	}

	@Override
	protected void autoDeployResources(ProcessEngine processEngine) {
		if (deploymentResources != null && deploymentResources.length > 0) {
			for (Resource resource : deploymentResources) {
				RepositoryService repositoryService = processEngine.getRepositoryService();
				String resourceName = resource.getFilename();
				try {
					DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
							.enableDuplicateFiltering().name(resourceName);
					if (resourceName.endsWith(".bar") || resourceName.endsWith(".zip")
							|| resourceName.endsWith(".jar")) {
						deploymentBuilder.addZipInputStream(new ZipInputStream(resource.getInputStream()));
					} else {
						deploymentBuilder.addInputStream(resourceName, resource.getInputStream());
					}
					deploymentBuilder.deploy();
				} catch (IOException e) {
					throw new FlowableException("couldn't auto deploy resource '" + resource + "': " + e.getMessage(),
							e);
				}

			}
		}
	}

}
