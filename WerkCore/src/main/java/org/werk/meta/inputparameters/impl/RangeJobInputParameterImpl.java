package org.werk.meta.inputparameters.impl;

import org.werk.meta.inputparameters.RangeJobInputParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.ParameterType;

import lombok.Getter;

public class RangeJobInputParameterImpl extends JobInputParameterImpl implements RangeJobInputParameter {
	@Getter
	protected Parameter start;
	@Getter
	protected Parameter end;
	@Getter
	protected boolean startInclusive;
	@Getter
	protected boolean endInclusive;
	@Getter
	protected boolean prohibitRange;
	
	public RangeJobInputParameterImpl(String name, ParameterType type, boolean isOptional, String description,
			Parameter start, Parameter end, boolean startInclusive, boolean endInclusive, boolean prohibitRange) {
		super(name, type, isOptional, description);
		this.start = start;
		this.end = end;
		this.startInclusive = startInclusive;
		this.endInclusive = endInclusive;
		this.prohibitRange = prohibitRange;
	}

	@Override
	public String getConstraints() {
		return String.format(
				"%s%sRange: [%s %s - %s %s]",
				isOptional() ? "Optional " : "",
				prohibitRange ? "Prohibit " : "",
				startInclusive ? "IN" : "EX", getParameterValue(start),
				endInclusive ? "IN" : "EX", getParameterValue(end)
			);
	}
	
	@Override
	public String toString() {
		return String.format(
			"%s%s %s [%sRange: %s %s - %s %s]",
			isOptional() ? "Optional " : "",
			type.toString(), name,
			prohibitRange ? "Prohibit " : "",
			startInclusive ? "IN" : "EX", getParameterValue(start),
			endInclusive ? "IN" : "EX", getParameterValue(end)
		);
	}
}
