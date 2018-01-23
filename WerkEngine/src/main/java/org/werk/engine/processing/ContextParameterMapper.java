package org.werk.engine.processing;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.werk.config.annotations.inject.JobParameter;
import org.werk.config.annotations.inject.StepParameter;
import org.werk.engine.WerkParameterException;
import org.werk.engine.processing.mapped.MappedBoolParameter;
import org.werk.engine.processing.mapped.MappedDictionaryParameter;
import org.werk.engine.processing.mapped.MappedDoubleParameter;
import org.werk.engine.processing.mapped.MappedListParameter;
import org.werk.engine.processing.mapped.MappedLongParameter;
import org.werk.engine.processing.mapped.MappedStringParameter;
import org.werk.parameters.interfaces.Parameter;

public class ContextParameterMapper {
	public static void remapParameters(JobContext jobContext, StepContext stepContext, Object obj) throws WerkParameterException {
		for (Field field : obj.getClass().getDeclaredFields()) {
			JobParameter jp = null;
			try {
				jp = field.getAnnotation(JobParameter.class);
			} catch(NullPointerException npe) {}
			
			StepParameter sp = null;
			try {
				sp = field.getAnnotation(StepParameter.class);
			} catch(NullPointerException npe) {}
			
			if ((jp != null) && (sp != null)) {
				throw new WerkParameterException(
						String.format("Class [%s] Field [%s] is annotated as both @JobParameter and @StepParameter", 
								obj.getClass(), field.getName())
					);
			}
			
			if ((jp != null) || (sp != null)) {
				@SuppressWarnings("rawtypes")
				Class fieldClass = field.getType();
				
				Parameter prm;
				if (fieldClass.equals(int.class) || fieldClass.equals(Integer.class) 
						|| fieldClass.equals(long.class) || fieldClass.equals(Long.class)) {
					prm = new MappedLongParameter(field, obj);
				} else if (fieldClass.equals(double.class) || fieldClass.equals(Double.class)) {
					prm = new MappedDoubleParameter(field, obj);
				} else if (fieldClass.equals(boolean.class) || fieldClass.equals(Boolean.class)) {
					prm = new MappedBoolParameter(field, obj);
				} else if (fieldClass.equals(String.class)) {
					prm = new MappedStringParameter(field, obj);
				} else if (fieldClass.equals(List.class)) {
					prm = new MappedListParameter(field, obj);
				} else if (fieldClass.equals(Map.class)) {
					prm = new MappedDictionaryParameter(field, obj);
				} else throw new WerkParameterException(
					String.format("Class [%s] Field [%s] is annotated as @%s, " + 
							"but its type is not allowed [%s]", 
							obj.getClass(), field.getName(), sp == null ? "JobParameter" : "StepParameter", 
							fieldClass)
				);
				
				if (sp != null) {
					String name = sp.name() == null ? null : sp.name().trim();
					if ((name == null) || (name.equals("")))
						name = field.getName();
					
					stepContext.putStepParameter(name, prm);
				} else {
					String name = jp.name() == null ? null : jp.name().trim();
					if ((name == null) || (name.equals("")))
						name = field.getName();
					
					jobContext.putJobParameter(name, prm);
				}
			}
		}
	}
}
