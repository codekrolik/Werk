package org.werk.ui.controls.parameters;

import java.io.IOException;

import org.werk.meta.inputparameters.DefaultValueJobInputParameter;
import org.werk.meta.inputparameters.JobInputParameter;
import org.werk.processing.parameters.LongParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.impl.LongParameterImpl;
import org.werk.ui.controls.parameters.state.PrimitiveParameterInit;
import org.werk.ui.guice.LoaderFactory;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import lombok.Getter;

public class LongParameterInput extends ParameterInput {
	@FXML
	protected TextField textField;
	@Getter
	PrimitiveParameterInit parameterInit;
	
	public LongParameterInput(PrimitiveParameterInit parameterInit) {
        this.parameterInit = parameterInit;
        parameterInit.setParameterInput(this);
        
        FXMLLoader fxmlLoader = LoaderFactory.getInstance().loader(getClass().getResource("LongParameterInput.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	/*public void updateDisabled() {
		textField.setEditable(getParameterInit().isImmutable());
	}*/
	
	public void initialize() {
		textField.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        if (!newValue.matches("\\d*"))
		            textField.setText(newValue.replaceAll("[^\\d]", ""));
		        try {
		        	Long l = Long.parseLong(textField.getText());
		        	if (parameterInit.getState() == null)
		        		parameterInit.setState(new LongParameterImpl(l));
		        	else
		        		((LongParameterImpl)parameterInit.getState()).setValue(l);
		        } catch(Exception e) { 
		        	if (parameterInit.getState() != null)
		        		((LongParameterImpl)parameterInit.getState()).setValue(null);
		        }
		    }
		});
		
		if (((LongParameter)parameterInit.getState()).getValue() != null) {
			restoreState((LongParameter)parameterInit.getState());
		} else {
			resetValue();
		}
	}

	public void resetValue() {
		updateDisabled();
		if (parameterInit.getOldParameter().isPresent()) {
			restoreState((LongParameter)parameterInit.getOldParameter().get());
		} else if (parameterInit.getJobInputParameter().isPresent()) {
			JobInputParameter jip = parameterInit.getJobInputParameter().get();
			if (jip instanceof DefaultValueJobInputParameter) {
				DefaultValueJobInputParameter defaultPrm = (DefaultValueJobInputParameter)jip;
				restoreState((LongParameter)defaultPrm.getDefaultValue());
			}
		} else
			parameterInit.setState(new LongParameterImpl(null));
	}

	protected void restoreState(LongParameter prm) {
		updateDisabled();
		if (prm.getValue() != null)
			textField.setText(prm.getValue().toString());
	}
	
	@Override
	public Parameter getParameter() {
		return parameterInit.getState();
	}
}
