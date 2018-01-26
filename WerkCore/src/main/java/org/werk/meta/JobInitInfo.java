package org.werk.meta;

import java.util.Map;
import java.util.Optional;

import org.werk.processing.parameters.Parameter;

public interface JobInitInfo {
	String getJobTypeName();
	Map<String, Parameter> getInitParameters();
	Optional<String> getJobName();
}