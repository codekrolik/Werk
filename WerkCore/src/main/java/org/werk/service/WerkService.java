package org.werk.service;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.werk.data.JobPOJO;
import org.werk.meta.JobInitInfo;
import org.werk.meta.JobReviveInfo;
import org.werk.meta.JobType;
import org.werk.meta.OldVersionJobInitInfo;
import org.werk.meta.StepType;
import org.werk.processing.readonly.ReadOnlyJob;

public interface WerkService<J> {
	//JOB LIFECYCLE MANAGEMENT
	J createJob(JobInitInfo init) throws Exception;
	J createOldVersionJob(OldVersionJobInitInfo init) throws Exception;
	void reviveJob(JobReviveInfo<J> jobReviveInfo) throws Exception;
	
	//JOB RETRIEVAL
	ReadOnlyJob<J> getJobAndHistory(J jobId) throws Exception;
	Collection<JobPOJO<J>> getJobs(Optional<Date> from, Optional<Date> to, 
			Optional<Set<String>> jobTypes, Optional<Collection<J>> jobIds) throws Exception;
	
	Collection<JobPOJO<J>> getChildJobs(J jobId) throws Exception;
	Collection<ReadOnlyJob<J>> getChildJobsAndHistory(J jobId) throws Exception;
	
	//JOB METADATA RETRIEVAL
	Collection<JobType> getJobTypes();
	JobType getJobType(String jobTypeName, Optional<Long> version);
	
	Collection<StepType<J>> getStepTypesForJob(String jobTypeName, Optional<Long> version);
	StepType<J> getStepType(String stepTypeName);
}