package org.werk.processing.parameters.impl;

import java.util.HashMap;
import java.util.Map;

import org.werk.processing.parameters.DictionaryParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.ParameterType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class DictionaryParameterImpl implements DictionaryParameter {
	@Getter @Setter
	protected Map<String, Parameter> value;
	
	public DictionaryParameterImpl(Map<String, Parameter> value) {
		this.value = value;
	}
	
	public DictionaryParameterImpl() {
		this.value = new HashMap<>();
	}
	
	@Override
	public ParameterType getType() {
		return ParameterType.DICTIONARY;
	}
}
