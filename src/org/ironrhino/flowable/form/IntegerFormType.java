package org.ironrhino.flowable.form;

import org.springframework.stereotype.Component;

@Component
public class IntegerFormType extends NamedFormType {

	private static final long serialVersionUID = -4357806528338598904L;

	@Override
	public Object convertFormValueToModelValue(String propertyValue) {
		try {
			return Integer.valueOf(propertyValue);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}