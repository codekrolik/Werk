package org.werk.processing.jobs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.werk.meta.JobInitInfo;
import org.werk.meta.JobReviveInfo;
import org.werk.meta.OldVersionJobInitInfo;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.readonly.ReadOnlyJob;
import org.werk.processing.steps.JoinResult;
import org.werk.processing.steps.Step;

public interface Job extends ReadOnlyJob {
	//String getJobTypeName();
	
	//Processing
	/**
	 * @return Read-only map of init parameters
	 */
	//Map<String, Parameter> getJobInitialParameters();
	Parameter getJobInitialParameter(String parameterName);
	
	/**
	 * @return Read-only map of job parameters
	 */
	//Map<String, Parameter> getJobParameters();
	Parameter getJobParameter(String parameterName);
	Parameter removeJobParameter(String parameterName);
	void putJobParameter(String parameterName, Parameter parameter);
	
	Long getLongParameter(String parameterName);
	void putLongParameter(String parameterName, Long value);
	Double getDoubleParameter(String parameterName);
	void putDoubleParameter(String parameterName, Double value);
	Boolean getBoolParameter(String parameterName);
	void putBoolParameter(String parameterName, Boolean value);
	String getStringParameter(String parameterName);
	void putStringParameter(String parameterName, String value);
	Map<String, Parameter> getDictionaryParameter(String parameterName);
	void putDictionaryParameter(String parameterName, Map<String, Parameter> value);
	List<Parameter> getListParameter(String parameterName);
	void putListParameter(String parameterName, List<Parameter> value);
	
	//JobStatus getStatus();
	void setStatus(JobStatus status);
	
	Step getCurrentStep();
	
	void openTempContext();
	void openTempContextAndRemap(Object obj);
	void commitTempContext();
	void rollbackTempContext();
	
	//-------------------------------------------------------------------
	
	JobToken fork(JobInitInfo jobInitInfo) throws Exception;
	JobToken forkOldVersion(OldVersionJobInitInfo jobInitInfo) throws Exception;
	
	void revive(JobReviveInfo jobReviveInfo) throws Exception;
	
	List<JobToken> getCreatedJobs();
	
	String tokenToStr(JobToken token);
	JobToken strToToken(String token);
	
	String joinResultToStr(JoinResult joinResult);
	JoinResult strToJoinResult(String joinResultStr);
	
	ReadOnlyJob loadJob(JobToken token);
	List<ReadOnlyJob> loadJobs(Collection<JobToken> tokens);
	List<ReadOnlyJob> loadAllChildJobs();
	List<ReadOnlyJob> loadChildJobsOfTypes(Set<String> jobTypes);
	
	ReadOnlyJob loadJobAndHistory(JobToken token);
	List<ReadOnlyJob> loadJobsAndHistory(Collection<JobToken> token);
	List<ReadOnlyJob> loadAllChildJobsAndHistory();
	List<ReadOnlyJob> loadChildJobsOfTypesAndHistory(Set<String> jobTypes);
}
