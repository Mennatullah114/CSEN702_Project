import java.util.*;
import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainGUI extends Application {

    private TomasuloSimulator sim;

    // GUI data
    private ObservableList<GuiModels.RSRow> rsData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.ROBRow> robData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.RegRow> regData = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        sim = new TomasuloSimulator();
        
        // CRITICAL: Initialize data BEFORE creating tables
        initTables();
        
        Label lblRS = new Label("All Reservation Stations (Combined)");
        Label lblROB = new Label("Reorder Buffer");
        Label lblRegs = new Label("Register File");

        TableView<GuiModels.RSRow> rsTable = createRSTable();
        TableView<GuiModels.ROBRow> robTable = createROBTable();
        TableView<GuiModels.RegRow> regTable = createRegisterTable();
        
        TextArea programInput = new TextArea();
        programInput.setPrefHeight(150);
        programInput.setPromptText("Enter assembly instructions here...");
        
        Button loadProgramBtn = new Button("Load Program");
        loadProgramBtn.setOnAction(e -> {
            List<Instruction> prog = InstructionParser.parse(programInput.getText());
            sim.loadProgram(prog);
            refreshTables();
        });

        Button nextCycleBtn = new Button("Next Cycle");
        nextCycleBtn.setOnAction(e -> {
            sim.step();
            refreshTables();
        });

        VBox root = new VBox(
            15,
            programInput,
            loadProgramBtn,
            lblRS,
            rsTable,
            lblROB,
            robTable,
            lblRegs,
            regTable,
            nextCycleBtn
        );

        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 800);

        stage.setTitle("Tomasulo Simulator");
        stage.setScene(scene);
        stage.show();

        refreshTables();
    }

    /** INIT TABLE DATA **/
    private void initTables() {
        // RS – create a row for each station
        for (ReservationStation rs : sim.getAllStations()) {
            rsData.add(new GuiModels.RSRow(rs));
        }

        // Register file
        for (int i = 0; i < 16; i++)
            regData.add(new GuiModels.RegRow("R" + i));
        for (int i = 0; i < 16; i++)
            regData.add(new GuiModels.RegRow("F" + i));
    }


    /** UPDATE TABLES EACH CYCLE **/
    private void refreshTables() {
        // RS update - USE PROPERTY METHODS NOW
        for (GuiModels.RSRow row : rsData) {
            ReservationStation rs = row.rs;
            row.busyProperty().set(rs.busy ? "Yes" : "No");
            row.opProperty().set(rs.op == null ? "-" : rs.op);
            row.VjProperty().set(rs.Vj == null ? "-" : rs.Vj);
            row.VkProperty().set(rs.Vk == null ? "-" : rs.Vk);
            row.QjProperty().set(rs.Qj == null ? "-" : rs.Qj);
            row.QkProperty().set(rs.Qk == null ? "-" : rs.Qk);
            row.latencyProperty().set(Integer.toString(rs.latencyRemaining));
        }

        // ROB update - USE PROPERTY METHODS
        robData.clear();
        int idx = 0;
        for (ReorderBuffer.ROBEntry e : sim.rob.queue) {
            GuiModels.ROBRow r = new GuiModels.ROBRow(idx++);
            r.destProperty().set(e.dest == null ? "-" : e.dest);
            r.valueProperty().set("" + e.value);
            r.readyProperty().set(e.ready ? "Yes" : "No");
            robData.add(r);
        }

        // Register file update - USE PROPERTY METHODS
        for (GuiModels.RegRow r : regData) {
            RegisterFile.Register reg = sim.registers.get(r.getReg());
            if (reg != null) {
                r.valueProperty().set("" + reg.value);
                r.tagProperty().set(reg.tag == null ? "-" : reg.tag);
            }
        }
    }

    /** TABLE DEFINITIONS **/

    private TableView<GuiModels.RSRow> createRSTable() {
        TableView<GuiModels.RSRow> table = new TableView<>();
        table.setPrefHeight(200);

        table.getColumns().add(col("Name", "name", 90));
        table.getColumns().add(col("Busy", "busy", 50));
        table.getColumns().add(col("Op", "op", 80));
        table.getColumns().add(col("Vj", "Vj", 80));
        table.getColumns().add(col("Vk", "Vk", 80));
        table.getColumns().add(col("Qj", "Qj", 80));
        table.getColumns().add(col("Qk", "Qk", 80));
        table.getColumns().add(col("Latency", "latency", 80));
        
        table.setItems(rsData);

        return table;
    }

    private TableView<GuiModels.ROBRow> createROBTable() {
        TableView<GuiModels.ROBRow> table = new TableView<>();
        table.setPrefHeight(200);

        table.getColumns().add(col("Entry", "entry", 100));
        table.getColumns().add(col("Dest", "dest", 100));
        table.getColumns().add(col("Value", "value", 100));
        table.getColumns().add(col("Ready", "ready", 80));
        
        table.setItems(robData);

        return table;
    }

    private TableView<GuiModels.RegRow> createRegisterTable() {
        TableView<GuiModels.RegRow> table = new TableView<>();
        table.setPrefHeight(200);

        table.getColumns().add(col("Register", "reg", 100));
        table.getColumns().add(col("Value", "value", 120));
        table.getColumns().add(col("Tag", "tag", 120));
        
        table.setItems(regData);

        return table;
    }

    private <T> TableColumn<T, String> col(String name, String property, int width) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }

    public static void main(String[] args) {
        launch(args);
    }
}