package com.ricardo.hz.ui;

import com.hazelcast.core.EntryView;
import com.hazelcast.core.IMap;
import com.ricardo.hz.connection.HZClient;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main extends Application {

	private static final String LOCAL = "local";
	private static final String DEV = "dev";
	private static final String QA = "qa";
	private static final String STAGE = "stage";

	private TableView<IMapEntry> table;
	private Scene scene;
	private HBox buttonHb;
	private StackPane root;
	private Text actionStatus;
	public static ObservableList<IMapEntry> data;

	DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private HZClient hzClient;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	public static void main(String [] args) {

		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		format.setTimeZone(TimeZone.getTimeZone("EST"));
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

				(t.getTableView().getItems().get(
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
		actionStatus = new Text();
		actionStatus.setFill(Color.FIREBRICK);

		// Vbox
		VBox vbox = new VBox(20);
		vbox.setPadding(new Insets(25, 25, 25, 25));;
		vbox.getChildren().addAll(labelHb, table, buttonHb, actionStatus);

		//Add to root stackpane
		root.getChildren().add(vbox);

		// Scene
		scene = new Scene(root, 800, 550); // w x h
		primaryStage.setScene(scene);
		primaryStage.show();
		
	} // start()

	private String generateEntryStats(String key) {
		EntryView entry = hzClient.getSessionMap().getEntryView(key);
		String createTime = format.format(new Date(entry.getCreationTime()));
		String expTime = format.format(new Date(entry.getExpirationTime()));
		String ttl = Double.toString(entry.getTtl() / 1000);
		String lastAccessTime = format.format(new Date(entry.getLastAccessTime()));
		String lastStoredTime = format.format(new Date(entry.getLastStoredTime()));
		String lastUpdateTime = format.format(new Date(entry.getLastUpdateTime()));

		StringBuffer sb = new StringBuffer();
		sb.append("Creation Time : ").append(createTime).append("\nExpiration Time : ").append(expTime).append("\nTTL : ").append(ttl).append(" secs")
				.append("\nLast Access Time : ").append(lastAccessTime).append("\nLast Stored Time : ").append(lastStoredTime)
				.append("\nLast Updated Time : ").append(lastUpdateTime);
		return sb.toString();

	}

 	private class RowSelectChangeListener implements ChangeListener<Number> {

		@Override
		public void changed(ObservableValue<? extends Number> ov, 
				Number oldVal, Number newVal) {

			int ix = newVal.intValue();
			System.out.println("____ " + ov.toString());
			System.out.println(ov.getValue());

			if ((ix < 0) || (ix >= data.size())) {
	
				return; // invalid data
			}

			IMapEntry entry = data.get(ix);
			String stats = generateEntryStats(entry.getKey());
			actionStatus.setText(stats);

		}
	}

	
	private void addData() {

		IMap<String,String> map = hzClient.getSessionMap();
		List<IMapEntry> list = new ArrayList<>();
		map.forEach( (k,v) -> list.add(new IMapEntry(k,v)));
		data.addAll(list);
	}

	private ComboBox createEnvironmentComboBox() {
		ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(LOCAL, DEV, QA, STAGE));
		comboBox.setPromptText("Select Environment");
		comboBox.valueProperty().addListener( (x,oldVal,newVal) ->
			{

				ProgressIndicator pi = new ProgressIndicator();
				VBox box = new VBox(pi);
				box.setAlignment(Pos.CENTER);
				this.buttonHb.setDisable(true);
				root.getChildren().add(box);

				Task <Void>environmentWorker = createEnvironmentTask(newVal);

				pi.progressProperty().unbind();
				pi.progressProperty().bind(environmentWorker.workDoneProperty());

				environmentWorker.setOnSucceeded((workerStateEvent) -> {
					root.getChildren().remove(box);
					buttonHb.setDisable(false);
				});
				environmentWorker.setOnFailed((workerStateEvent) -> {
					root.getChildren().remove(box);
					buttonHb.setDisable(false);
				});
				new Thread(environmentWorker).start();
			});
		return comboBox;
	}

	private Task createEnvironmentTask(final String newVal) {
		Runnable timerTask = () -> System.out.println("Client is switching to " + newVal);
		FutureTask<Void> futureTask = new FutureTask<>(timerTask, null);
		Thread t = new Thread(futureTask);
		t.start();
		try {
			futureTask.get(10, SECONDS);  //let's timeout if we cannot connect within 10 seconds.
		}catch(Exception e) {
			e.printStackTrace();
		}

		return new Task() {

			@Override
			protected Object call() throws Exception {
				Thread t = new Thread(futureTask);
				t.start();
				hzClient.disconnect();
				data.clear();

				switch(newVal) {
					case LOCAL:
						hzClient.init("127.0.0.1:5702", "ums-hz", "ums-hz-pass");
						addData();
						break;
					case DEV :
						hzClient.init("sdc-nppf01-dev.vpymnts.net:5702", "ums-hz", "ums-hz-pass");
						addData();
						break;
					case QA :
						hzClient.init("sdc-nppf01-qa.vpymnts.net:5702", "ums-hz", "ums-hz-pass");
						addData();
						break;
					case STAGE :
						hzClient.init("sdc-pfapp01-stage.vpymnts.net", "ums-hz", "ums-hz-pass");
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

