package org.werk.ui.controls.parameters.state;

import java.util.Optional;

import org.werk.meta.inputparameters.DefaultValueJobInputParameter;
import org.werk.meta.inputparameters.JobInputParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.ParameterType;
import org.werk.ui.controls.parameters.ParameterInput;

import lombok.Getter;
import lombok.Setter;

public abstract class ParameterInit {
	@Getter
	protected Optional<JobInputParameter> jobInputParameter;
	@Getter
	protected Optional<Parameter> oldParameter;
	protected ParameterType type;
	@Getter @Setter
	protected ParameterInput parameterInput;
	@Setter
	protected Boolean immutable = null;
	
	public ParameterInit(ParameterType type) {
		jobInputParameter = Optional.empty();
		oldParameter = Optional.empty();
		this.type = type;
	}
	
	public ParameterInit(JobInputParameter jobInputPrm) {
		jobInputParameter = Optional.of(jobInputPrm);
		oldParameter = Optional.empty();
		this.type = null;
	}
	
	public ParameterInit(Parameter oldPrm) {
		jobInputParameter = Optional.empty();
		oldParameter = Optional.of(oldPrm);
		this.type = null;
	}
	
	public ParameterType getParameterType() {
		if (jobInputParameter.isPresent())
			return jobInputParameter.get().getType();
		else if (oldParameter.isPresent())
			return oldParameter.get().getType();
		else
			return type;
	}
	
	public boolean isImmutable() {
		if (immutable != null)
			return immutable;
		if (getJobInputParameter().isPresent()) {
			JobInputParameter jip = getJobInputParameter().get();
			if (jip instanceof DefaultValueJobInputParameter)
				return true;
			else
				return jip.isOptional();
		}
		return false;
	}
	
	public abstract Parameter getState() throws ParameterStateException;
}
