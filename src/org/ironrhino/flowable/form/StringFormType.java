package org.ironrhino.flowable.form;

public class StringFormType extends NamedFormType {

	private static final long serialVersionUID = 7285802771387869197L;

	@Override
	public Object convertFormValueToModelValue(String propertyValue) {
		return propertyValue;
	}

}