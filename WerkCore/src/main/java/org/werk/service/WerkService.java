package org.werk.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.pillar.time.interfaces.Timestamp;
import org.werk.meta.JobInitInfo;
import org.werk.meta.JobRestartInfo;
import org.werk.meta.JobType;
import org.werk.meta.JobTypeSignature;
import org.werk.meta.StepType;
import org.werk.meta.VersionJobInitInfo;
import org.werk.processing.jobs.JobStatus;
import org.werk.processing.readonly.ReadOnlyJob;

public interface WerkService<J> {
	//JOB CREATION/RESTART
	J createJob(JobInitInfo init) throws Exception;
	J createJobOfVersion(VersionJobInitInfo init) throws Exception;
	void restartJob(JobRestartInfo<J> jobRestartInfo) throws Exception;
	
	//JOB RETRIEVAL
	ReadOnlyJob<J> getJobAndHistory(J jobId) throws Exception;
	JobCollection<J> getJobs(Optional<Timestamp> from, Optional<Timestamp> to,
			Optional<Timestamp> fromExec, Optional<Timestamp> toExec,
			Optional<List<JobTypeSignature>> jobTypesAndVersions, Optional<Collection<J>> parentJobIds,
			Optional<Collection<J>> jobIds, Optional<Set<String>> currentStepTypes, 
			Optional<Set<JobStatus>> jobStatuses, Optional<PageInfo> pageInfo) throws Exception;
	
	//JOB METADATA RETRIEVAL
	Collection<JobType> getJobTypes();
	JobType getJobType(String jobTypeName, Optional<Long> version);
	Collection<JobType> getJobTypesForStep(String stepTypeName);
	
	Collection<StepType<J>> getAllStepTypes();
	Collection<StepType<J>> getStepTypesForJob(String jobTypeName, Optional<Long> version);
	StepType<J> getStepType(String stepTypeName);
	
	//NOTIFICATION
	void jobsAdded();
	
	JSONObject getServerInfo();
}