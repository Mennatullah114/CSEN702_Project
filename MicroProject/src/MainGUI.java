import java.util.*;

import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainGUI extends Application {

    private TomasuloSimulator sim;
    private SimulatorConfig config;

    // GUI data
    private ObservableList<GuiModels.RSRow> rsData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.RSRow> addSubData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.RSRow> mulDivData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.RSRow> loadStoreData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.ROBRow> robData = FXCollections.observableArrayList();
    private ObservableList<GuiModels.RegRow> regData = FXCollections.observableArrayList();
    
    private Stage primaryStage;
    private TextArea programInput;
    private TextArea cacheDisplay;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        config = new SimulatorConfig();
        sim = new TomasuloSimulator(config);
        
        // Set up cache miss listener
        sim.setCacheMissListener(address -> showCacheMissAlert(address));
        
        // Show configuration dialog first
        showConfigDialog(true);
    }
    
    private void showConfigDialog(boolean isInitial) {
        ConfigDialog dialog = new ConfigDialog(primaryStage, config);
        boolean okClicked = dialog.showAndWait();
        
        if (okClicked) {
            config = dialog.getConfig();
            
            if (isInitial) {
                // First time - create the GUI
                createGUI();
            } else {
                // Reconfigure - recreate simulator
                String currentProgram = programInput.getText();
                sim = new TomasuloSimulator(config);
                sim.setCacheMissListener(address -> showCacheMissAlert(address));
                reinitTables();
                if (!currentProgram.isEmpty()) {
                    // Reload program if there was one
                    List<Instruction> prog = InstructionParser.parse(currentProgram);
                    sim.loadProgram(prog);
                }
                refreshTables();
            }
        } else if (isInitial) {
            // User cancelled on first dialog - use defaults and continue
            createGUI();
        }
    }
    
    private void createGUI() {
        // Initialize data BEFORE creating tables
        initTables();
        
        Label lblAddSub = new Label("FP ADD / SUB Stations");
        Label lblMulDiv = new Label("FP MUL / DIV Stations");
        Label lblLoadStore = new Label("Load / Store Buffers");
        Label lblROB = new Label("Reorder Buffer");
        Label lblRegs = new Label("Register File");
        Label lblCache = new Label("Cache Status");

        TableView<GuiModels.RSRow> addSubTable = createRSTable(addSubData);
        TableView<GuiModels.RSRow> mulDivTable = createRSTable(mulDivData);
        TableView<GuiModels.RSRow> loadStoreTable = createRSTable(loadStoreData);
        TableView<GuiModels.ROBRow> robTable = createROBTable();
        TableView<GuiModels.RegRow> regTable = createRegisterTable();
        
        programInput = new TextArea();
        programInput.setPrefHeight(100);
        programInput.setPromptText("Enter assembly instructions here...");
        
        cacheDisplay = new TextArea();
        cacheDisplay.setPrefHeight(100);
        cacheDisplay.setEditable(false);
        cacheDisplay.setStyle("-fx-font-family: monospace;");
        
        Button configBtn = new Button("Configuration");
        configBtn.setOnAction(e -> showConfigDialog(false));
        
        Button initRegBtn = new Button("Initialize Registers");
        initRegBtn.setOnAction(e -> showRegisterInitDialog());
        
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
        
        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            sim = new TomasuloSimulator(config);
            sim.setCacheMissListener(address -> showCacheMissAlert(address));
            reinitTables();
            refreshTables();
        });
        
        HBox buttonBox = new HBox(10, configBtn, initRegBtn, loadProgramBtn, nextCycleBtn, resetBtn);

        VBox root = new VBox(
            10,
            programInput,
            buttonBox,
            lblAddSub,
            addSubTable,
            lblMulDiv,
            mulDivTable,
            lblLoadStore,
            loadStoreTable,
            lblROB,
            robTable,
            lblRegs,
            regTable,
            lblCache,
            cacheDisplay
        );

        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 1050, 900);

        primaryStage.setTitle("Tomasulo Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshTables();
    }
    
    private void showRegisterInitDialog() {
        RegisterInitDialog dialog = new RegisterInitDialog(primaryStage, sim.registers);
        if (dialog.showAndWait()) {
            refreshTables();
        }
    }

    /** INIT TABLE DATA **/
    private void initTables() {
        rsData.clear();
        addSubData.clear();
        mulDivData.clear();
        loadStoreData.clear();
        regData.clear();
        
        // RS – create a row for each station and separate by type
        for (ReservationStation rs : sim.fpAddStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            addSubData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.fpMulStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            mulDivData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.loadBuffers) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            loadStoreData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.intStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            loadStoreData.add(row);
            rsData.add(row);
        }

        // Register file
        for (int i = 0; i < 16; i++)
            regData.add(new GuiModels.RegRow("R" + i));
        for (int i = 0; i < 16; i++)
            regData.add(new GuiModels.RegRow("F" + i));
    }
    
    private void reinitTables() {
        rsData.clear();
        addSubData.clear();
        mulDivData.clear();
        loadStoreData.clear();
        
        for (ReservationStation rs : sim.fpAddStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            addSubData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.fpMulStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            mulDivData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.loadBuffers) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            loadStoreData.add(row);
            rsData.add(row);
        }
        
        for (ReservationStation rs : sim.intStations) {
            GuiModels.RSRow row = new GuiModels.RSRow(rs);
            loadStoreData.add(row);
            rsData.add(row);
        }
    }


    /** UPDATE TABLES EACH CYCLE **/
    private void refreshTables() {
        // RS update
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

        // ROB update
        robData.clear();
        int idx = 0;
        for (ReorderBuffer.ROBEntry e : sim.rob.queue) {
            GuiModels.ROBRow r = new GuiModels.ROBRow(idx++);
            r.destProperty().set(e.dest == null ? "-" : e.dest);
            r.valueProperty().set("" + e.value);
            r.readyProperty().set(e.ready ? "Yes" : "No");
            robData.add(r);
        }

        // Register file update
        for (GuiModels.RegRow r : regData) {
            RegisterFile.Register reg = sim.registers.get(r.getReg());
            if (reg != null) {
                // Format the value based on register type
                String valueStr;
                if (r.getReg().startsWith("F")) {
                    // Floating point register - show with decimals
                    valueStr = String.format("%.4f", reg.value);
                } else {
                    // Integer register - show as integer
                    valueStr = String.format("%d", (int)reg.value);
                }
                r.valueProperty().set(valueStr);
                r.tagProperty().set(reg.tag == null ? "-" : reg.tag);
            }
        }
        
        // Cache update
        updateCacheDisplay();
    }
    
    private void updateCacheDisplay() {
        List<String> cacheStatus = sim.cache.getCacheStatus();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Cache: %d bytes, Block: %d bytes, Blocks: %d\n", 
                  sim.config.cacheSize, sim.config.blockSize, 
                  sim.config.cacheSize / sim.config.blockSize));
        sb.append("----------------------------------------\n");
        
        int validBlocks = 0;
        for (String status : cacheStatus) {
            if (status.contains("Valid")) {
                sb.append(status).append("\n");
                validBlocks++;
            }
        }
        
        if (validBlocks == 0) {
            sb.append("(Cache is empty)\n");
        }
        
        cacheDisplay.setText(sb.toString());
    }
    
    private void showCacheMissAlert(int address) {
        Stage alertStage = new Stage();
        alertStage.setTitle("Cache Miss");
        alertStage.initModality(Modality.APPLICATION_MODAL);  // Changed to modal - must press OK
        alertStage.initOwner(primaryStage);
        
        VBox alertBox = new VBox(15);
        alertBox.setPadding(new Insets(20));
        alertBox.setStyle("-fx-alignment: center; -fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-width: 2;");
        
        Label titleLabel = new Label("⚠ Cache Miss Detected");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #856404;");
        
        Label messageLabel = new Label(String.format("Address: %d\nFetching block from memory...", address));
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #856404;");
        
        Button okBtn = new Button("OK");
        okBtn.setPrefWidth(80);
        okBtn.setOnAction(ev -> alertStage.close());
        
        alertBox.getChildren().addAll(titleLabel, messageLabel, okBtn);
        
        alertStage.setScene(new Scene(alertBox, 320, 150));
        alertStage.showAndWait();  // Changed to showAndWait - blocks until closed
    }

    /** TABLE DEFINITIONS **/

    private TableView<GuiModels.RSRow> createRSTable(ObservableList<GuiModels.RSRow> data) {
        TableView<GuiModels.RSRow> table = new TableView<>();
        table.setPrefHeight(100);

        table.getColumns().add(col("Name", "name", 85));
        table.getColumns().add(col("Busy", "busy", 65));
        table.getColumns().add(col("Op", "op", 110));
        table.getColumns().add(col("Vj", "Vj", 85));
        table.getColumns().add(col("Vk", "Vk", 85));
        table.getColumns().add(col("Qj", "Qj", 85));
        table.getColumns().add(col("Qk", "Qk", 85));
        table.getColumns().add(col("Latency", "latency", 80));
        
        table.setItems(data);

        return table;
    }

    private TableView<GuiModels.ROBRow> createROBTable() {
        TableView<GuiModels.ROBRow> table = new TableView<>();
        table.setPrefHeight(120);

        table.getColumns().add(col("Entry", "entry", 100));
        table.getColumns().add(col("Dest", "dest", 100));
        table.getColumns().add(col("Value", "value", 120));
        table.getColumns().add(col("Ready", "ready", 80));
        
        table.setItems(robData);

        return table;
    }

    private TableView<GuiModels.RegRow> createRegisterTable() {
        TableView<GuiModels.RegRow> table = new TableView<>();
        table.setPrefHeight(120);

        table.getColumns().add(col("Register", "reg", 100));
        table.getColumns().add(col("Value", "value", 120));
        table.getColumns().add(col("Tag", "tag", 100));
        
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