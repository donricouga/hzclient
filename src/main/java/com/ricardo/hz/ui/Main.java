package com.ricardo.hz.ui;

import com.hazelcast.core.IMap;
import com.ricardo.hz.connection.HZClient;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.ArrayList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class Main extends Application {

	private static final String DEV = "dev";
	private static final String QA = "qa";

	private TableView<IMapEntry> table;
	private Scene scene;
	private HBox buttonHb;
	private StackPane root;
	public static ObservableList<IMapEntry> data;
	//private Text actionStatus;

	private HZClient hzClient;

	public static void main(String [] args) {

		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		hzClient = new HZClient();
		root = new StackPane();

		primaryStage.setTitle("IMap Viewer");

		Label label = new Label("Session IDs");
		label.setTextFill(Color.DARKBLUE);
		label.setFont(Font.font("Calibri", FontWeight.BOLD, 36));

		//Combo box to select environment to connect
		ComboBox<String> comboBox = createEnvironmentComboBox();
		VBox labelHb = new VBox();
		labelHb.setAlignment(Pos.CENTER);
		labelHb.getChildren().addAll(label, comboBox);

		// Table view, data, columns and properties

		table = new TableView<>();
		data = FXCollections.observableArrayList();
		table.setItems(data);

		TableColumn titleCol = new TableColumn("Key");
		titleCol.setCellValueFactory(new PropertyValueFactory<IMapEntry, String>("key"));
		titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
		titleCol.setOnEditCommit(new EventHandler<CellEditEvent<IMapEntry, String>>() {
			@Override
			public void handle(CellEditEvent<IMapEntry, String> t) {

				((IMapEntry) t.getTableView().getItems().get(
					t.getTablePosition().getRow())
				).setKey(t.getNewValue());
			}
		});

		TableColumn authorCol = new TableColumn("Value");
		authorCol.setCellValueFactory(new PropertyValueFactory<IMapEntry, String>("value"));
		authorCol.setCellFactory(TextFieldTableCell.forTableColumn());
		authorCol.setOnEditCommit(new EventHandler<CellEditEvent<IMapEntry, String>>() {
			@Override
			public void handle(CellEditEvent<IMapEntry, String> t) {
	
				((IMapEntry) t.getTableView().getItems().get(
					t.getTablePosition().getRow())
				).setValue(t.getNewValue());
			}
		});

		table.getColumns().setAll(titleCol, authorCol);
		table.setPrefWidth(450);
		table.setPrefHeight(300);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		table.getSelectionModel().selectedIndexProperty().addListener(
			new RowSelectChangeListener());

		// Add and delete buttons
		Button delbtn = new Button("Delete");
		delbtn.setOnAction(new DeleteButtonListener());
		buttonHb = new HBox(10);
		buttonHb.setAlignment(Pos.CENTER);
		buttonHb.getChildren().addAll(delbtn);

		// Status message text
//		actionStatus = new Text();
//		actionStatus.setFill(Color.FIREBRICK);

		// Vbox
		VBox vbox = new VBox(20);
		vbox.setPadding(new Insets(25, 25, 25, 25));;
		vbox.getChildren().addAll(labelHb, table, buttonHb);

		//Add to root stackpane
		root.getChildren().add(vbox);

		// Scene
		scene = new Scene(root, 800, 550); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();
		
	} // start()
	
 	private class RowSelectChangeListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> ov, 
				Number oldVal, Number newVal) {

			int ix = newVal.intValue();

			if ((ix < 0) || (ix >= data.size())) {
	
				return; // invalid data
			}

			IMapEntry IMapEntry = data.get(ix);
		}
	}

	
	private void addData() {

		IMap<String,String> map = hzClient.getSessionMap();
		List<IMapEntry> list = new ArrayList<>();
		map.forEach( (k,v) -> list.add(new IMapEntry(k,v)));
		data.addAll(list);
	}

	private ComboBox createEnvironmentComboBox() {
		ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(DEV, QA));
		comboBox.setPromptText("Select Environment");
		comboBox.valueProperty().addListener( (x,oldVal,newVal) ->
			{

				ProgressIndicator pi = new ProgressIndicator();
				VBox box = new VBox(pi);
				box.setAlignment(Pos.CENTER);
				this.buttonHb.setDisable(true);
				root.getChildren().add(box);

				Task environmentWorker = createEnvironmentTask(newVal, box);

				pi.progressProperty().unbind();
				pi.progressProperty().bind(environmentWorker.workDoneProperty());

				environmentWorker.setOnSucceeded((workerStateEvent) -> {
					root.getChildren().remove(box);
					buttonHb.setDisable(false);
				});
				new Thread(environmentWorker).start();
			});
		return comboBox;
	}

	private Task createEnvironmentTask(final String newVal, VBox box) {
		return new Task() {

			@Override
			protected Object call() throws Exception {
				hzClient.disconnect();
				data.clear();

				switch(newVal) {
					case DEV :
						hzClient.init("sdc-nppf01-dev.vpymnts.net:5702", "ums-hz", "ums-hz-pass");
						addData();
						break;
					case QA :
						hzClient.init("sdc-nppf01-qa.vpymnts.net:5702", "ums-hz", "ums-hz-pass");
						addData();
						break;
					default:
						System.out.println("SHOULDN'T HAPPEN!");

				}
				return true;
			}
		};
	}

	private class DeleteButtonListener implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent e) {

			// Get selected row and delete
			int ix = table.getSelectionModel().getSelectedIndex();
			IMapEntry IMapEntry = table.getSelectionModel().getSelectedItem();
			data.remove(ix);

			//Remove from Hazelcast
			hzClient.deleteEntry(IMapEntry.getKey());

			//actionStatus.setText("Deleted: " + IMapEntry.toString());

//			// Select a row
//			if (table.getItems().size() == 0) {
//				actionStatus.setText("No data in table !");
//				return;
//			}

			if (ix != 0) {
				ix = ix -1;
			}

			table.requestFocus();
			table.getSelectionModel().select(ix);
			table.getFocusModel().focus(ix);
		}
	}
}
