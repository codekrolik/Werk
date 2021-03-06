package org.werk.meta;

import org.werk.processing.steps.StepExec;

public interface StepExecFactory<J> {
	StepExec<J> createStepExec() throws Exception;
	@SuppressWarnings("rawtypes")
	Class getStepExecClass();
	String getStepExecClassName();
}
