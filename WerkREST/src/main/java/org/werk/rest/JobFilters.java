package org.werk.rest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.pillar.time.interfaces.Timestamp;
import org.werk.meta.JobTypeSignature;
import org.werk.processing.jobs.JobStatus;
import org.werk.service.PageInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class JobFilters<J> {
	@Getter
	Optional<Timestamp> from;
	@Getter
	Optional<Timestamp> to; 
	@Getter
	Optional<Timestamp> fromExec;
	@Getter
	Optional<Timestamp> toExec; 
	@Getter
	Optional<List<JobTypeSignature>> jobTypesAndVersions;
	@Getter
	Optional<Collection<J>> parentJobIds; 
	@Getter
	Optional<Collection<J>> jobIds;
	@Getter
	Optional<Set<String>> currentStepTypes;
	@Getter
	Optional<Set<JobStatus>> jobStatuses;
	@Getter
	Optional<PageInfo> pageInfo;
}
