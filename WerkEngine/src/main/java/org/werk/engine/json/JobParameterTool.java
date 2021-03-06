package org.werk.engine.json;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.werk.exceptions.WerkException;
import org.werk.meta.JobType;
import org.werk.meta.inputparameters.DefaultValueJobInputParameter;
import org.werk.meta.inputparameters.EnumJobInputParameter;
import org.werk.meta.inputparameters.JobInputParameter;
import org.werk.meta.inputparameters.RangeJobInputParameter;
import org.werk.processing.parameters.BoolParameter;
import org.werk.processing.parameters.DictionaryParameter;
import org.werk.processing.parameters.DoubleParameter;
import org.werk.processing.parameters.ListParameter;
import org.werk.processing.parameters.LongParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.ParameterType;
import org.werk.processing.parameters.StringParameter;
import org.werk.util.ParameterUtils;

public class JobParameterTool {
	public void fillParameters(List<JobInputParameter> parameterSet, Map<String, Parameter> jobInitialParameters) {
		for (JobInputParameter parameter : parameterSet) {
			if (!jobInitialParameters.containsKey(parameter.getName())) {
				if (parameter instanceof DefaultValueJobInputParameter) {
					Parameter defaultValue = ((DefaultValueJobInputParameter)parameter).getDefaultValue();
					jobInitialParameters.put(parameter.getName(), defaultValue);
				}
			}
		}
	}
	
	public void checkParameters(List<JobInputParameter> parameterSet,
			Map<String, Parameter> jobInitialParameters) throws WerkException {
		checkDictionaryParameters(jobInitialParameters);
		checkRangeAndEnumParameters(parameterSet, jobInitialParameters);
	}
	
	protected void checkDictionaryParameters(Map<String, Parameter> jobInitialParameters) throws WerkException {
		for (Entry<String, Parameter> ent : jobInitialParameters.entrySet()) {
			Parameter p = ent.getValue();
			if (p.getType() == ParameterType.LONG) {
				if (((LongParameter)p).getValue() == null)
					throw new WerkException(String.format("LONG parameter %s must have a value", ent.getKey()));
			} else if (p.getType() == ParameterType.DOUBLE) {
				if (((DoubleParameter)p).getValue() == null)
					throw new WerkException(String.format("DOUBLE parameter %s must have a value", ent.getKey()));
			} else if (p.getType() == ParameterType.BOOL) {
				if (((BoolParameter)p).getValue() == null)
					throw new WerkException(String.format("BOOL parameter %s must have a value", ent.getKey()));
			} else if (p.getType() == ParameterType.STRING) {
				if (((StringParameter)p).getValue() == null)
					throw new WerkException(String.format("STRING parameter %s must have a value", ent.getKey()));
			} else if (p.getType() == ParameterType.LIST) {
				List<Parameter> list = ((ListParameter)p).getValue();
				if ((list == null) || (list.isEmpty()))
					throw new WerkException(String.format("LIST parameter %s must not be empty", ent.getKey()));
				checkListParameters(list);
			} else if (p.getType() == ParameterType.DICTIONARY) {
				Map<String, Parameter> dict = ((DictionaryParameter)p).getValue();
				if ((dict == null) || (dict.isEmpty()))
					throw new WerkException(String.format("DICTIONARY parameter %s must not be empty", ent.getKey()));
				checkDictionaryParameters(dict);
			} else
				throw new WerkException("Unknown ParameterType " + p.getType());
		}
	}
	
	protected void checkListParameters(List<Parameter> jobInitialParameters) throws WerkException {
		for (Parameter p : jobInitialParameters) {
			if (p.getType() == ParameterType.LONG) {
				if (((LongParameter)p).getValue() == null)
					throw new WerkException(String.format("LONG parameter must have a value"));
			} else if (p.getType() == ParameterType.DOUBLE) {
				if (((DoubleParameter)p).getValue() == null)
					throw new WerkException(String.format("DOUBLE parameter must have a value"));
			} else if (p.getType() == ParameterType.BOOL) {
				if (((BoolParameter)p).getValue() == null)
					throw new WerkException(String.format("BOOL parameter must have a value"));
			} else if (p.getType() == ParameterType.STRING) {
				if (((StringParameter)p).getValue() == null)
					throw new WerkException(String.format("STRING parameter must have a value"));
			} else if (p.getType() == ParameterType.LIST) {
				List<Parameter> list = ((ListParameter)p).getValue();
				if ((list == null) || (list.isEmpty()))
					throw new WerkException(String.format("LIST parameter must not be empty"));
				checkListParameters(list);
			} else if (p.getType() == ParameterType.DICTIONARY) {
				Map<String, Parameter> dict = ((DictionaryParameter)p).getValue();
				if ((dict == null) || (dict.isEmpty()))
					throw new WerkException(String.format("DICTIONARY parameter must not be empty"));
				checkDictionaryParameters(dict);
			} else
				throw new WerkException("Unknown ParameterType " + p.getType());
		}
	}
	
	protected boolean parametersEqual(Parameter p1, Parameter p2) {
		if (p1.getType() != p2.getType())
			return false;
		return ParameterUtils.getParameterValue(p1).equals( ParameterUtils.getParameterValue(p2) );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected int parametersCompare(Parameter p1, Parameter p2) throws WerkException {
		if (p1.getType() != p2.getType())
			throw new WerkException(
				String.format("Can't compare parameters of different types [%s] [%s]", p1.getType(), p2.getType())
			);
		
		return ((Comparable)ParameterUtils.getParameterValue(p1)).compareTo( ParameterUtils.getParameterValue(p2) );
	}
	
	protected void checkRangeAndEnumParameters(List<JobInputParameter> parameterSet, 
			Map<String, Parameter> jobInitialParameters) throws WerkException {
		for (JobInputParameter parameter : parameterSet) {
			if (jobInitialParameters.containsKey(parameter.getName())) {
				Parameter jobPrm = jobInitialParameters.get(parameter.getName());
				
				if (parameter instanceof EnumJobInputParameter) {
					boolean match = false;
					for (Parameter p : ((EnumJobInputParameter)parameter).getValues()) {
						if (parametersEqual(p, jobPrm)) { 
							match = true;
							break;
						}
					}
					
					if (((EnumJobInputParameter)parameter).isProhibitValues()) {
						if (match)
							throw new WerkException(
								String.format("Enum value is prohibited for value [%s], param [%s] ", 
										ParameterUtils.getParameterValue(jobPrm), parameter)
							);
					} else {
						if (!match)
							throw new WerkException(
								String.format("Enum match not found for value [%s], param [%s]", 
										ParameterUtils.getParameterValue(jobPrm), parameter)
							);
					}
				} else if (parameter instanceof RangeJobInputParameter) {
					Parameter start = ((RangeJobInputParameter)parameter).getStart();
					Parameter end = ((RangeJobInputParameter)parameter).getEnd();
					
					if (parametersCompare(start, end) > 0) {
						Parameter tmp = start;
						start = end;
						end = tmp;
					}
					
					boolean startConstraint;
					if (((RangeJobInputParameter)parameter).isStartInclusive())
						startConstraint = (parametersCompare(start, jobPrm) <= 0);
					else
						startConstraint = (parametersCompare(start, jobPrm) < 0);
					
					boolean endConstraint;
					if (((RangeJobInputParameter)parameter).isEndInclusive())
						endConstraint = (parametersCompare(jobPrm, end) <= 0);
					else
						endConstraint = (parametersCompare(jobPrm, end) < 0);
					
					if (startConstraint && endConstraint) {
						if (((RangeJobInputParameter)parameter).isProhibitRange())
							throw new WerkException(
								String.format("Parameter value in prohibited range [%s - %s]; name [%s], value [%s]", 
										ParameterUtils.getParameterValue(start), ParameterUtils.getParameterValue(end), 
										parameter.getName(), ParameterUtils.getParameterValue(jobPrm))
							);
					} else {
						if (!((RangeJobInputParameter)parameter).isProhibitRange())
							throw new WerkException(
								String.format("Parameter value outside of allowed range [%s - %s]; name [%s], value [%s]", 
										ParameterUtils.getParameterValue(start), ParameterUtils.getParameterValue(end), 
										parameter.getName(), ParameterUtils.getParameterValue(jobPrm))
							);
					}
				}
			}
		}
	}

	/*public List<JobInputParameter> findMatchingParameterSet(JobType jobType, Map<String, Parameter> parameters) throws WerkConfigException {
		boolean match = false;
		for (List<JobInputParameter> allowedParameters : jobType.getInitParameters().values()) {
			//Check that all required parameters exist
			boolean allRequiredParametersExist = checkAllRequiredParametersExist(allowedParameters, parameters);
			
			if (allRequiredParametersExist) {
				//Check that no unknown parameters are present
				boolean noUnknownParametersExist = checkNoUnknownParametersExist(allowedParameters, parameters);
				
				if (noUnknownParametersExist)
					match = true;
			}
			
			if (match)
				return allowedParameters;
		}
		
		throw new WerkConfigException(
				String.format("Can't create JobType [%s] - match for input parameter set not found", 
					jobType.getJobTypeName())
			);
	}*/
	
	public boolean emptyInitParameterSetAllowed(JobType jobType) {
		return (jobType.getInitParameters() == null || jobType.getInitParameters().isEmpty());
	}
	
	public List<JobInputParameter> getParameterSet(JobType jobType, String initSignatureName) {
		return jobType.getInitParameters().get(initSignatureName);
	}
	
	protected boolean isParameterRequired(JobInputParameter ip) {
		if (ip.isOptional())
			return false;
		
		return !(ip instanceof DefaultValueJobInputParameter);
	}
	
	protected boolean compareParameters(JobInputParameter parameterType, Parameter ip) {
		if (!ip.getType().equals(parameterType.getType()))
			return false;
		
		if (ip instanceof DefaultValueJobInputParameter)
			if (((DefaultValueJobInputParameter)ip).isDefaultValueImmutable())
				if (!ParameterUtils.getParameterValue(((DefaultValueJobInputParameter)ip).getDefaultValue()
						).equals( ParameterUtils.getParameterValue(ip) ))
					return false;
		
		return true;
	}
	
	protected boolean checkAllRequiredParametersExist(List<JobInputParameter> allowedParameters, 
			Map<String, Parameter> inputParameters) {
		for (JobInputParameter parameterType : allowedParameters) {
			if (isParameterRequired(parameterType)) {
				Parameter ip = inputParameters.get(parameterType.getName());
				if (ip == null)
					return false;
				
				if (!compareParameters(parameterType, ip))
					return false;
			}
		}
		return true;
	}
	
	protected boolean checkNoUnknownParametersExist(List<JobInputParameter> allowedParameters, 
			Map<String, Parameter> inputParameters) {
		Map<String, JobInputParameter> allowedParametersMap = 
				allowedParameters.stream().collect(Collectors.toMap(i -> i.getName(), i -> i));
		
		for (Entry<String, Parameter> ent : inputParameters.entrySet()) {
			String parameterName = ent.getKey();
			Parameter parameter = ent.getValue();
			
			JobInputParameter parameterType = allowedParametersMap.get(parameterName);
			if (parameterType == null)
				return false;
			
			if (!compareParameters(parameterType, parameter))
				return false;
		}
		
		return true;
	}
}
