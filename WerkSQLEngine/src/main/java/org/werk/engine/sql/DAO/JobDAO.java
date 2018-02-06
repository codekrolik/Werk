package org.werk.engine.sql.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.pillar.db.interfaces.TransactionContext;
import org.pillar.db.jdbc.JDBCTransactionContext;
import org.pillar.db.jdbc.JDBCTransactionFactory;
import org.pillar.time.LongTimeProvider;
import org.pillar.time.LongTimestamp;
import org.pillar.time.interfaces.TimeProvider;
import org.pillar.time.interfaces.Timestamp;
import org.werk.config.WerkConfig;
import org.werk.data.StepPOJO;
import org.werk.engine.json.JobParameterTool;
import org.werk.engine.json.ParameterContextSerializer;
import org.werk.engine.sql.exception.JobReviveException;
import org.werk.engine.sql.exception.JoinStateException;
import org.werk.exceptions.WerkConfigException;
import org.werk.meta.JobInitInfo;
import org.werk.meta.JobReviveInfo;
import org.werk.meta.JobType;
import org.werk.meta.OldVersionJobInitInfo;
import org.werk.meta.inputparameters.JobInputParameter;
import org.werk.processing.jobs.JobStatus;
import org.werk.processing.jobs.JoinStatusRecord;
import org.werk.processing.jobs.JoinStatusRecordImpl;
import org.werk.processing.parameters.Parameter;

public class JobDAO {
	protected TimeProvider timeProvider; 
	protected ParameterContextSerializer parameterContextSerializer;
	
	protected WerkConfig<Long> werkConfig;
	protected StepDAO stepDAO;
	protected JobParameterTool jobParameterTool = new JobParameterTool();
	
	public JobDAO(TimeProvider timeProvider, ParameterContextSerializer parameterContextSerializer, 
			WerkConfig<Long> werkConfig, StepDAO stepDAO) {
		this.timeProvider = timeProvider;
		this.parameterContextSerializer = parameterContextSerializer;
		this.werkConfig = werkConfig;
		this.stepDAO = stepDAO;
	}
	
	public Long updateFirstStep(TransactionContext tc, long jobId, long newStepId) throws Exception {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			pst = connection.prepareStatement("UPDATE jobs id_job = ?, current_step_id = ?, step_count = ?");
			
			pst.setLong(1, jobId);
			pst.setLong(2, newStepId);
			pst.setInt(3, 1);
			
			pst.executeUpdate();
			
			long serverId = JDBCTransactionFactory.getLastId(connection);
			return serverId;
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	public Long createJob(TransactionContext tc, JobInitInfo init, int jobStatus, Optional<Long> parentJob, 
			int stepCount) throws Exception {
		JobType jobType = werkConfig.getJobTypeLatestVersion(init.getJobTypeName());
		if (jobType == null)
			throw new WerkConfigException(
				String.format("JobType not found [%s]", init.getJobTypeName())
			);
		
		//Check initial parameters
		List<JobInputParameter> parameterSet = jobParameterTool.findMatchingParameterSet(jobType, init.getInitParameters());
		
		//Check enum and range parameters
		jobParameterTool.checkRangeAndEnumParameters(parameterSet, init.getInitParameters());
		
		//Fill default parameters
		jobParameterTool.fillParameters(parameterSet, init.getInitParameters());
		
		return createJob(tc, init.getJobTypeName(), jobType.getVersion(), init.getJobName(), 
				parentJob, jobStatus, ((LongTimestamp)timeProvider.getCurrentTime()).getTimeMs(), 
				init.getInitParameters(), stepCount);
	}
	
	public Long createOldVersionJob(TransactionContext tc, OldVersionJobInitInfo init, int jobStatus, Optional<Long> parentJob, 
			int stepCount) throws Exception {
		JobType jobType = werkConfig.getJobTypeForOldVersion(init.getOldVersion(), init.getJobTypeName());
		if (jobType == null)
			throw new WerkConfigException(
				String.format("JobType not found [%s] for version [%d]", init.getJobTypeName(), init.getOldVersion())
			);
		
		//Check initial parameters
		List<JobInputParameter> parameterSet = jobParameterTool.findMatchingParameterSet(jobType, init.getInitParameters());
		
		//Check enum and range parameters
		jobParameterTool.checkRangeAndEnumParameters(parameterSet, init.getInitParameters());
		
		//Fill default parameters
		jobParameterTool.fillParameters(parameterSet, init.getInitParameters());
		
		return createJob(tc, init.getJobTypeName(), init.getOldVersion(), init.getJobName(), 
				parentJob, jobStatus, ((LongTimestamp)timeProvider.getCurrentTime()).getTimeMs(), 
				init.getInitParameters(), stepCount);
	}
	
	public Long createJob(TransactionContext tc, String jobTypeName, long version, Optional<String> jobName,
			Optional<Long> parentJobId, int status, long nextExecutionTime, Map<String, Parameter> jobInitialParameters, 
			int stepCount) throws Exception {
		String jobInitialParameterState = parameterContextSerializer.serializeParameters(jobInitialParameters).toString();
		
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			String query = "INSERT INTO jobs (job_type, version, job_name, parent_job_id, " +
					"status, next_execution_time, job_parameter_state, job_initial_parameter_state, step_count) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			pst = connection.prepareStatement(query);
			
			pst.setString(1, jobTypeName);
			pst.setLong(2, version);
			
			if (jobName.isPresent())
				pst.setString(3, jobName.get());
			else
				pst.setNull(3, java.sql.Types.VARCHAR);
			
			if (parentJobId.isPresent())
				pst.setLong(4, parentJobId.get());
			else
				pst.setNull(4, java.sql.Types.BIGINT);
			
			pst.setInt(5, status);
			pst.setLong(6, nextExecutionTime);
			pst.setString(7, jobInitialParameterState);
			pst.setString(8, jobInitialParameterState);
			pst.setLong(9, stepCount);
			
			pst.executeUpdate();
			
			long serverId = JDBCTransactionFactory.getLastId(connection);
			return serverId;
		} finally {
			if (pst != null) pst.close();
		}
	}

	/*
	current_step_id
	status
	step_count
	next_execution_time
	job_parameter_state

	wait_for_N_jobs
	status_before_join
	join_parameter_name
	*/
	public int updateJob(TransactionContext tc, long jobId, long currentStepId, JobStatus status, Timestamp nextExecutionTime,
		Map<String, Parameter> jobParameters, int stepCount, Optional<JoinStatusRecord<Long>> joinStatusRecord) throws Exception {
		if (joinStatusRecord.isPresent()) {
			DBJobPOJO job = loadJob(tc, jobId);
			if (job == null)
				throw new JoinStateException(
						String.format("Job not found: [%d]", jobId)
					);
			
			if (job.getStatus() == JobStatus.JOINING)
				throw new JoinStateException(
						String.format("Job is already in JOINING state: [%d]", jobId)
					);
		}
		
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			String query;
			if (joinStatusRecord.isPresent()) {
				query = "UPDATE jobs SET current_step_id = ?, status = ?, next_execution_time = ?, job_parameter_state = ?, " + 
						"step_count = ?, wait_for_N_jobs = ?, status_before_join = ?, join_parameter_name = ? " + 
						"WHERE id_job = ?";
			} else {
				query = "UPDATE jobs SET current_step_id = ?, status = ?, next_execution_time = ?, job_parameter_state = ?, " + 
						"step_count = ? " + 
						"WHERE id_job = ?";
			}
			
			pst = connection.prepareStatement(query);
			
			String jobParametersStr = parameterContextSerializer.serializeParameters(jobParameters).toString();
			
			pst.setLong(1, currentStepId);
			pst.setInt(2, status.getCode());
			pst.setLong(3, ((LongTimestamp)nextExecutionTime).getTimeMs());
			pst.setString(4, jobParametersStr);
			pst.setInt(5, stepCount);
			
			if (joinStatusRecord.isPresent()) {
				if (joinStatusRecord.get().getWaitForNJobs().isPresent())
					pst.setLong(6, joinStatusRecord.get().getWaitForNJobs().get());
				else
					pst.setNull(6, java.sql.Types.BIGINT);
				
				pst.setInt(7, joinStatusRecord.get().getStatusBeforeJoin().getCode());
				pst.setString(8, joinStatusRecord.get().getJoinParameterName());
			}
			
			int recordsUpdated = pst.executeUpdate();
			
			if (joinStatusRecord.isPresent())
				for (Long joinedJobId : joinStatusRecord.get().getJoinedJobs())
					recordsUpdated += createJoinRecordJob(tc, jobId, joinedJobId);
			
			return recordsUpdated;
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	public int createJoinRecordJob(TransactionContext tc, long awaitingJobId, long jobId) throws SQLException {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			String query = "INSERT INTO join_record_jobs (id_awaiting_job, id_job) " +
					"VALUES (?, ?)";
			
			pst = connection.prepareStatement(query);
			
			pst.setLong(1, awaitingJobId);
			pst.setLong(2, jobId);
			
			return pst.executeUpdate();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	public void reviveJob(TransactionContext tc, JobReviveInfo<Long> init) throws Exception {
		PreparedStatement pst = null;
		
		try {
			//1. Load job from DB
			long jobId = init.getJobId();
			DBJobPOJO job = loadJob(tc, jobId);
			
			if (job == null)
				throw new JobReviveException(
						String.format("Job not found: [%d]", jobId)
					);
			
			if ((job.getStatus() != JobStatus.FINISHED) && (job.getStatus() != JobStatus.ROLLED_BACK) && 
				(job.getStatus() != JobStatus.FAILED))
				throw new JobReviveException(
						String.format("Job can't be revived: not in final state: [%d] [%s]", jobId, job.getStatus())
					);
			
			//2. Set updated job parameters
			Map<String, Parameter> jobParameters = job.getJobParameters();
			
			for (Map.Entry<String, Parameter> ent : init.getJobParametersUpdate().entrySet())
				jobParameters.put(ent.getKey(), ent.getValue());
			for (String jobParameterToRemove : init.getJobParametersToRemove())
				jobParameters.remove(jobParameterToRemove);
			
			JobStatus jobStatus = job.getStatus();
			long newStepId = job.getCurrentStepId();
			
			//3. Create new step or update existing step
			if (init.getNewStepTypeName().isPresent()) {
				//3.1 Create new step

				//Init step parameters
				Map<String, Parameter> stepParameters = init.getStepParametersUpdate();
				for (String stepParameterToRemove : init.getStepParametersToRemove())
					stepParameters.remove(stepParameterToRemove);
				
				//Create step
				if (init.isNewStepRollback().get()) {
					jobStatus = JobStatus.ROLLING_BACK;
					newStepId = stepDAO.createRollbackStep(tc, jobId, init.getNewStepTypeName().get(), job.stepCount++, 
						init.getStepParametersUpdate(), init.getStepsToRollback());
				} else {
					jobStatus = JobStatus.PROCESSING;
					newStepId = stepDAO.createProcessingStep(tc, jobId, init.getNewStepTypeName().get(), job.stepCount++, 
						init.getStepParametersUpdate());
				}
			} else {
				//3.2 Load and update existing step
				StepPOJO step = stepDAO.getStep(tc, jobId, job.getCurrentStepId());
				
				//Set updated step parameters
				for (Map.Entry<String, Parameter> ent : init.getStepParametersUpdate().entrySet())
					step.getStepParameters().put(ent.getKey(), ent.getValue());
				for (String stepParameterToRemove : init.getStepParametersToRemove())
					step.getStepParameters().remove(stepParameterToRemove);
				
				//Update step
				stepDAO.updateStep(tc, job.getCurrentStepId(), step.getExecutionCount(), 
						step.getStepParameters(), step.getProcessingLog());
			}
			
			this.updateJob(tc, jobId, newStepId, jobStatus, job.getNextExecutionTime(), 
					job.getJobParameters(), job.getStepCount(), job.getJoinStatusRecord());
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	public DBJobPOJO loadJob(TransactionContext tc, long jobId) throws SQLException {
		List<Long> jobIds = new ArrayList<>();
		jobIds.add(jobId);
		Collection<DBJobPOJO> jobs = loadJobs(tc, Optional.empty(), Optional.empty(), Optional.of(jobIds), 
				Optional.empty(), Optional.empty());
		
		if ((jobs == null) || (jobs.isEmpty()))
			return null;
		
		for (DBJobPOJO job : jobs)
			return job;
		
		return null;
	}
	
	public Collection<DBJobPOJO> loadJobs(TransactionContext tc, Optional<Timestamp> from, Optional<Timestamp> to, 
			Optional<Collection<Long>> jobIds, Optional<Long> parentJobId, Optional<Set<String>> jobTypes) throws SQLException {
		Connection connection = ((JDBCTransactionContext)tc).getConnection();
		PreparedStatement pst = null;
		
		try {
			StringBuilder sb = new StringBuilder("SELECT j.id_job, j.job_type, j.version, j.job_name, j.parent_job_id, " + 
					"j.current_step_id, j.status, j.next_execution_time, j.job_parameter_state, j.job_initial_parameter_state, " + 
					"j.id_locker, j.step_count, j.wait_for_N_jobs, j.status_before_join, j.join_parameter_name, r.id_job " + 
					"FROM jobs j LEFT JOIN join_record_jobs r ON j.id_job = r.id_awaiting_job");
			
			if (from.isPresent() || to.isPresent() || (jobIds.isPresent() && !jobIds.get().isEmpty()) ||
					parentJobId.isPresent() || jobTypes.isPresent())
				sb.append("WHERE ");
			
			int count = 0;
			if (from.isPresent()) {
				if (count++ > 0) sb.append(" AND");
				sb.append(" next_execution_time >= ? ");
			}
			if (to.isPresent()) {
				if (count++ > 0) sb.append(" AND");
				sb.append(" next_execution_time <= ? ");
			}
			if (jobIds.isPresent() && !jobIds.get().isEmpty()) {
				if (count++ > 0) sb.append(" AND");
				sb.append(" id_job IN (");
				
				int jobCount = 0;
				for (@SuppressWarnings("unused") long jobId : jobIds.get()) {
					if (jobCount++ > 0) sb.append(", ");
					sb.append("?");
				}
				
				sb.append(")");
			}
			if (parentJobId.isPresent()) {
				if (count++ > 0) sb.append(" AND");
				sb.append(" parent_job_id = ? ");
			}
			if (jobTypes.isPresent()) {
				if (count++ > 0) sb.append(" AND");
				sb.append(" job_type IN (");
				
				int jobTypeCount = 0;
				for (@SuppressWarnings("unused") String jobType : jobTypes.get()) {
					if (jobTypeCount++ > 0) sb.append(", ");
					sb.append("?");
				}
				
				sb.append(")");
			}
			
			pst = connection.prepareStatement(sb.toString());
			
			count = 0;
			if (from.isPresent())
				pst.setLong(++count, ((LongTimestamp)from.get()).getTimeMs());
			if (to.isPresent())
				pst.setLong(++count, ((LongTimestamp)to.get()).getTimeMs());
			if (jobIds.isPresent() && !jobIds.get().isEmpty())
				for (long jobId : jobIds.get())
					pst.setLong(++count, jobId);
			if (parentJobId.isPresent())
				pst.setLong(++count, parentJobId.get());
			if (jobTypes.isPresent())
				for (String jobType : jobTypes.get())
					pst.setString(++count, jobType);
			
			ResultSet rs = pst.executeQuery();
			
			Map<Long, DBJobPOJO> jobs = new HashMap<Long, DBJobPOJO>();
			while (rs.next())
				loadJobPOJO(rs, jobs);
			
			return jobs.values();
		} finally {
			if (pst != null) pst.close();
		}
	}
	
	protected void loadJobPOJO(ResultSet rs, Map<Long, DBJobPOJO> jobs) throws SQLException {
		DBJobPOJO dbJobPOJO;
		long jobId = rs.getLong(1);
		
		if (!jobs.containsKey(jobId)) {
			String jobTypeName = rs.getString(2);
			long version = rs.getLong(3);
			
			String jobNameStr = rs.getString(4);
			Optional<String> jobName;
			if (rs.wasNull())
				jobName = Optional.empty();
			else
				jobName = Optional.of(jobNameStr);
			
			long parentJobIdLong = rs.getLong(5);
			Optional<Long> parentJobId;
			if (rs.wasNull())
				parentJobId = Optional.empty();
			else
				parentJobId = Optional.of(parentJobIdLong);
			
			long currentStepId = rs.getLong(6);
			
			JobStatus status = JobStatus.fromCode(rs.getInt(7));
			Timestamp nextExecutionTime = ((LongTimeProvider)timeProvider).createTimestamp(rs.getLong(8));
			
			String jobParametersStr = rs.getString(9);
			Map<String, Parameter> jobParameters = 
					parameterContextSerializer.deserializeParameters(new JSONObject(jobParametersStr));
			
			String jobInitialParametersStr = rs.getString(10);
			Map<String, Parameter> jobInitialParameters = 
					parameterContextSerializer.deserializeParameters(new JSONObject(jobInitialParametersStr));
			
			long idLocker = rs.getLong(11);
			
			int stepCount = rs.getInt(12);

			Optional<JoinStatusRecord<Long>> joinStatusRecord;
			String joinParameterName = rs.getString(15);
			if (rs.wasNull()) {
				joinStatusRecord = Optional.empty();
			} else {
				long waitForNJobsLong = rs.getLong(13);
				Optional<Long> waitForNJobs;
				if (rs.wasNull())
					waitForNJobs = Optional.empty();
				else
					waitForNJobs = Optional.of(waitForNJobsLong);
				
				JobStatus statusBeforeJoin = JobStatus.fromCode(rs.getInt(14));
				
				long joinedJobId = rs.getLong(16);
				List<Long> joinedJobs = new ArrayList<Long>();
				joinedJobs.add(joinedJobId);
				
				JoinStatusRecord<Long> jsr = new JoinStatusRecordImpl<>(joinedJobs, joinParameterName, statusBeforeJoin, 
						waitForNJobs);
				joinStatusRecord = Optional.of(jsr);
			}
			
			dbJobPOJO = new DBJobPOJO(jobTypeName, version, jobId, jobName, parentJobId, stepCount,
					currentStepId, jobInitialParameters, status, nextExecutionTime, 
					jobParameters, joinStatusRecord, idLocker);
			jobs.put(jobId, dbJobPOJO);
		} else {
			dbJobPOJO = (DBJobPOJO)jobs.get(jobId);
			JoinStatusRecord<Long> jsr = dbJobPOJO.joinStatusRecord.get();
			
			long joinedJobId = rs.getLong(16);
			if (!rs.wasNull())
				jsr.getJoinedJobs().add(joinedJobId);
		}
	}
}