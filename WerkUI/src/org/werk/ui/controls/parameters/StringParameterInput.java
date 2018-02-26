package org.werk.ui.controls.parameters;

import java.io.IOException;

import org.werk.meta.inputparameters.DefaultValueJobInputParameter;
import org.werk.meta.inputparameters.JobInputParameter;
import org.werk.processing.parameters.Parameter;
import org.werk.processing.parameters.StringParameter;
import org.werk.processing.parameters.impl.StringParameterImpl;
import org.werk.ui.controls.parameters.state.PrimitiveParameterInit;
import org.werk.ui.guice.LoaderFactory;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;

public class StringParameterInput extends ParameterInput {
	PrimitiveParameterInit parameterInit;
	
	@FXML
	TextField textField;
	
	public StringParameterInput(PrimitiveParameterInit parameterInit) {
        this.parameterInit = parameterInit;
        
        FXMLLoader fxmlLoader = LoaderFactory.getInstance().loader(getClass().getResource("StringParameterInput.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void initialize() {
		textField.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		        try {
		        	String str = textField.getText();
		        	if (parameterInit.getState() == null)
		        		parameterInit.setState(new StringParameterImpl(str));
		        	else
		        		((StringParameterImpl)parameterInit.getState()).setValue(str);
		        } catch(Exception e) { }
		    }
		});
		
		if (parameterInit.getState() != null) {
			restoreState((StringParameter)parameterInit.getState());
		} else {
			if (parameterInit.getOldParameter().isPresent()) {
				restoreState((StringParameter)parameterInit.getOldParameter().get());
			} else if (parameterInit.getJobInputParameter().isPresent()) {
				JobInputParameter jip = parameterInit.getJobInputParameter().get();
				if (jip instanceof DefaultValueJobInputParameter) {
					DefaultValueJobInputParameter defaultPrm = (DefaultValueJobInputParameter)jip;
					restoreState((StringParameter)defaultPrm.getDefaultValue());
					if (defaultPrm.isDefaultValueImmutable())
						setImmutable();
				}
			} else
				parameterInit.setState(new StringParameterImpl(null));
		}
	}
	
	protected void restoreState(StringParameter prm) {
		if (prm.getValue() != null)
			textField.setText(prm.getValue());
	}
	
	public void setImmutable() {
		textField.setDisable(true);
	}
	
	@Override
	public Parameter getParameter() {
		return parameterInit.getState();
	}
}
