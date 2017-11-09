package org.ironrhino.flowable.form;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.FormType;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.form.BooleanFormType;
import org.flowable.engine.impl.form.DateFormType;
import org.flowable.engine.impl.form.EnumFormType;
import org.flowable.engine.impl.form.LongFormType;
import org.flowable.engine.impl.form.StringFormType;
import org.flowable.engine.repository.ProcessDefinition;
import org.ironrhino.common.support.DictionaryControl;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;

import lombok.Getter;

@Component
public class FormRenderer {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired(required = false)
	private List<PersistableFormType<?>> persistableFormTypeList;

	@Autowired(required = false)
	private List<org.ironrhino.flowable.form.EnumFormType<?>> enumFormTypeList;

	@Autowired
	private CheckboxFormType checkboxFormType;

	public Map<String, FormElement> render(StartFormData startFormData) {
		return render(startFormData.getFormProperties());
	}

	public Map<String, FormElement> render(TaskFormData taskFormData) {
		return render(taskFormData.getFormProperties());
	}

	@SuppressWarnings("unchecked")
	protected Map<String, FormElement> render(List<FormProperty> formProperties) {
		ValueStack vs = null;
		HttpServletRequest request = ServletActionContext.getRequest();
		if (ActionContext.getContext() != null)
			vs = ActionContext.getContext().getValueStack();
		Map<String, FormElement> elements = new LinkedHashMap<String, FormElement>();
		for (FormProperty fp : formProperties) {
			if (vs != null && fp.getValue() != null)
				vs.set(fp.getId(), fp.getValue());
			FormElement fe = new FormElement();
			elements.put(fp.getId(), fe);
			fe.setValue(fp.getValue());
			if (StringUtils.isBlank(fe.getValue()) && request != null
					&& StringUtils.isNotBlank(request.getParameter(fp.getId())))
				fe.setValue(request.getParameter(fp.getId()));
			String label = fp.getName();
			if (StringUtils.isBlank(label))
				label = fp.getId();
			fe.setLabel(label);
			if (fp.isRequired()) {
				fe.setRequired(fp.isRequired());
				fe.addCssClass("required");
			}
			if (!fp.isWritable())
				fe.setDisabled(true);
			FormType type = fp.getType();
			if (type instanceof EnumFormType) {
				fe.setType("select");
				fe.setValues((Map<String, String>) type.getInformation("values"));
			} else if (type instanceof DateFormType) {
				DateFormType dft = (DateFormType) type;
				String datePattern = (String) dft.getInformation("datePattern");
				fe.addCssClass(
						datePattern.startsWith("HH") ? "time" : (datePattern.contains("HH") ? "datetime" : "date"));
				fe.setDynamicAttribute("data-format", datePattern);
			} else if (type instanceof BooleanFormType) {
				fe.setType("radio");
				Map<String, String> values = new LinkedHashMap<String, String>();
				values.put("true", "true");
				values.put("false", "false");
				fe.setValues(values);
			} else if (type instanceof LongFormType || type instanceof IntegerFormType) {
				fe.setInputType("number");
				fe.addCssClass("integer");
			} else if (type instanceof StringFormType) {
			} else if (type instanceof TextareaFormType) {
				fe.setType("textarea");
				fe.addCssClass("input-xxlarge");
			} else if (type instanceof DictionaryFormType) {
				try {
					DictionaryControl dc = applicationContext.getBean(DictionaryControl.class);
					fe.setType("select");
					Map<String, String> map = dc.getItemsAsMap(fp.getId());
					for (Map.Entry<String, String> entry : map.entrySet()) {
						if (StringUtils.isBlank(entry.getValue())) {
							String value = I18N.getText(entry.getKey());
							entry.setValue(value);
						}
					}
					fe.setValues(map);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else if (type instanceof CheckboxFormType) {
				try {
					if (fe.getValue() != null)
						fe.setArrayValues(
								(String[]) ((CheckboxFormType) type).convertFormValueToModelValue(fe.getValue()));
					DictionaryControl dc = applicationContext.getBean(DictionaryControl.class);
					fe.setType("checkbox");
					Map<String, String> map = dc.getItemsAsMap(fp.getId());
					for (Map.Entry<String, String> entry : map.entrySet()) {
						if (StringUtils.isBlank(entry.getValue())) {
							String value = I18N.getText(entry.getKey());
							entry.setValue(value);
						}
					}
					fe.setValues(map);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else if (type instanceof BigDecimalFormType) {
				fe.setInputType("number");
				if (fp.getId().toLowerCase().endsWith("rate")) {
					fe.getDynamicAttributes().put("data-scale", "8");
					fe.getDynamicAttributes().put("step", "0.00000001");
				}
				fe.addCssClass("double");
			} else if (type instanceof PersistableFormType) {
				PersistableFormType<?> pft = (PersistableFormType<?>) type;
				fe.setType("listpick");
				fe.getDynamicAttributes().put("pickUrl", pft.getPickUrl());
			} else if (type instanceof org.ironrhino.flowable.form.EnumFormType) {
				org.ironrhino.flowable.form.EnumFormType<?> eft = (org.ironrhino.flowable.form.EnumFormType<?>) type;
				fe.setType("enum");
				fe.getDynamicAttributes().put("enumType", eft.getEnumType().getName());
			}
		}
		return elements;
	}

	public Map<String, String> display(String processDefinitionId, String activityId, Map<String, String> data) {
		List<Property> formProperties;
		try {
			formProperties = getFormProperties(processDefinitionId, activityId);
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
		if (formProperties == null || formProperties.isEmpty())
			return data;
		Map<String, String> map = new LinkedHashMap<String, String>();
		Set<String> set = new HashSet<>();
		for (Property fp : formProperties) {
			String value = data.get(fp.getId());
			if (value == null)
				continue;
			set.add(fp.getId());
			String name = fp.getName();
			if (StringUtils.isBlank(name))
				name = fp.getId();
			String type = fp.getType();
			if ("boolean".equalsIgnoreCase(type)) {
				value = I18N.getText(value);
			} else if ("enum".equalsIgnoreCase(type)) {
				Map<String, String> temp = fp.getValues();
				if (temp != null) {
					String v = temp.get(value);
					if (StringUtils.isNotBlank(v))
						value = v;
				}
			} else if ("dictionary".equalsIgnoreCase(type)) {
				try {
					DictionaryControl dc = applicationContext.getBean(DictionaryControl.class);
					Map<String, String> temp = dc.getItemsAsMap(fp.getId());
					if (temp != null) {
						String v = temp.get(value);
						if (StringUtils.isNotBlank(v))
							value = v;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else if ("checkbox".equalsIgnoreCase(type)) {
				try {
					DictionaryControl dc = applicationContext.getBean(DictionaryControl.class);
					Map<String, String> temp = dc.getItemsAsMap(fp.getId());
					if (temp != null) {
						String[] arr = (String[]) checkboxFormType.convertFormValueToModelValue(value);
						for (int i = 0; i < arr.length; i++) {
							String v = temp.get(arr[i]);
							if (StringUtils.isNotBlank(v))
								arr[i] = v;
						}
						value = StringUtils.join(arr, CheckboxFormType.DELIMITER_FOR_DISPLAY);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				boolean found = false;
				if (persistableFormTypeList != null) {
					for (PersistableFormType<?> ft : persistableFormTypeList) {
						if (ft.getName().equals(type)) {
							Object v = ft.convertFormValueToModelValue(value);
							if (v != null)
								value = v.toString();
							found = true;
							break;
						}
					}
				}
				if (!found && enumFormTypeList != null) {
					for (org.ironrhino.flowable.form.EnumFormType<?> ft : enumFormTypeList) {
						if (ft.getName().equals(type)) {
							Object v = ft.convertFormValueToModelValue(value);
							if (v != null)
								value = v.toString();
							found = true;
							break;
						}
					}
				}
			}
			map.put(name, value);
		}
		for (Map.Entry<String, String> entry : data.entrySet()) {
			if (!set.contains(entry.getKey()))
				map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

	Map<String, List<Property>> cache = new ConcurrentHashMap<>();

	private List<Property> getFormProperties(String processDefinitionId, String activityId) throws Exception {
		String key = new StringBuilder(activityId).append("@").append(processDefinitionId).toString();
		List<Property> list = cache.get(key);
		if (list == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			list = new ArrayList<Property>();
			ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
					.processDefinitionId(processDefinitionId).singleResult();
			if (pd != null) {

				try (InputStream resourceAsStream = repositoryService.getResourceAsStream(pd.getDeploymentId(),
						pd.getResourceName())) {
					DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
					docBuilderFactory.setNamespaceAware(true);
					Document doc = docBuilderFactory.newDocumentBuilder()
							.parse(new InputSource(new InputStreamReader(resourceAsStream, "UTF-8")));
					Element processElement = (Element) doc.getDocumentElement().getElementsByTagName("process").item(0);
					Element elut = null;
					NodeList nl = processElement.getElementsByTagName("startEvent");
					for (int i = 0; i < nl.getLength(); i++) {
						Element ele = (Element) nl.item(i);
						if (activityId.equals(ele.getAttribute("id"))) {
							elut = ele;
							break;
						}
					}
					if (elut == null) {
						nl = processElement.getElementsByTagName("userTask");
						for (int i = 0; i < nl.getLength(); i++) {
							Element ele = (Element) nl.item(i);
							if (activityId.equals(ele.getAttribute("id"))) {
								elut = ele;
								break;
							}
						}
					}
					if (elut != null) {
						nl = elut.getElementsByTagName("extensionElements");
						if (nl.getLength() > 0) {
							Element extensionElements = (Element) nl.item(0);
							nl = extensionElements.getElementsByTagNameNS("http://flowable.org/bpmn", "formProperty");
							for (int i = 0; i < nl.getLength(); i++) {
								list.add(new Property((Element) nl.item(i)));
							}
						}
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			cache.put(key, list);
		}
		return list;
	}

	private static class Property implements Serializable {

		private static final long serialVersionUID = 3962888136575115773L;

		@Getter
		private String id;
		@Getter
		private String name;
		@Getter
		private String type;
		@Getter
		private Map<String, String> values;

		public Property(Element element) {
			this.id = element.getAttribute("id");
			this.name = element.getAttribute("name");
			this.type = element.getAttribute("type");
			if ("enum".equals(this.type)) {
				NodeList nl = element.getElementsByTagName("value");
				values = new LinkedHashMap<String, String>();
				for (int i = 0; i < nl.getLength(); i++) {
					Element ele = (Element) nl.item(i);
					values.put(ele.getAttribute("id"), ele.getAttribute("name"));
				}
			}
		}

	}

}
