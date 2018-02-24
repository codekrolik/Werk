package org.werk.ui.controls.steptypesform;

import java.io.IOException;
import java.util.Collection;

import org.werk.meta.StepType;
import org.werk.restclient.WerkCallback;
import org.werk.restclient.WerkRESTClient;
import org.werk.ui.ServerInfoManager;
import org.werk.ui.controls.mainapp.MainApp;
import org.werk.ui.guice.LoaderFactory;
import org.werk.ui.util.MessageBox;

import com.google.inject.Inject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class StepTypesForm extends VBox {
	@FXML
    Button refreshButton;
	@FXML
	StepTypesTable table;
	
	@Inject
	MainApp mainApp;
	@Inject
	WerkRESTClient werkClient;
	@Inject
	ServerInfoManager serverInfoManager;
	
	public StepTypesForm() {
        FXMLLoader fxmlLoader = LoaderFactory.getInstance().loader(getClass().getResource("StepTypesForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
	}
	
	public void initTable() {
		table.setMainApp(mainApp);
	}
    
	@FXML
    public void refresh() {
		if (serverInfoManager.getPort() < 0)
			MessageBox.show(String.format("Server not assigned. Please set server."));
		else {
			String host = serverInfoManager.getHost();
			int port = serverInfoManager.getPort();
			
			refreshButton.setDisable(true);
			
			WerkCallback<Collection<StepType<Long>>> callback = new WerkCallback<Collection<StepType<Long>>>() {
				@Override
				public void error(Throwable cause) {
					Platform.runLater( () -> {
						refreshButton.setDisable(false);
						MessageBox.show(
							String.format("Error processing request %s:%d [%s]", host, port, cause.toString())
						);
					});
				}
				
				@Override
				public void done(Collection<StepType<Long>> result) {
					Platform.runLater( () -> {
						refreshButton.setDisable(false);
						table.setItems(FXCollections.observableArrayList(result));
					});
				}
			};
			
			werkClient.getAllStepTypes(host, port, callback);
		}
	}
}
