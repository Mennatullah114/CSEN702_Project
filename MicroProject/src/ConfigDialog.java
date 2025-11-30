import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class ConfigDialog {

    private SimulatorConfig config;
    private Stage dialogStage;
    private boolean okClicked = false;

    // Station size fields
    private TextField fpAddField, fpMulField, loadField, intField;

    // Latency fields
    private TextField addSubLatField, mulLatField, divLatField;
    private TextField loadLatField, storeLatField, intLatField;

    // Cache fields
    private TextField cacheSizeField, blockSizeField;
    private TextField cacheHitField, cacheMissField;

    public ConfigDialog(Stage owner, SimulatorConfig currentConfig) {
        this.config = new SimulatorConfig();
        if (currentConfig != null) {
            copyConfig(currentConfig, this.config);
        }

        // Create the custom dialog as a Stage
        dialogStage = new Stage();
        dialogStage.setTitle("Simulator Configuration");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setResizable(false);

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // === Reservation Station Sizes ===
        grid.add(createSectionLabel("Reservation Station Sizes"), 0, row++, 2, 1);

        grid.add(new Label("FP ADD/SUB Stations:"), 0, row);
        fpAddField = new TextField(String.valueOf(config.fpAddStations));
        grid.add(fpAddField, 1, row++);

        grid.add(new Label("FP MUL/DIV Stations:"), 0, row);
        fpMulField = new TextField(String.valueOf(config.fpMulStations));
        grid.add(fpMulField, 1, row++);

        grid.add(new Label("Load Buffers:"), 0, row);
        loadField = new TextField(String.valueOf(config.loadBuffers));
        grid.add(loadField, 1, row++);

        grid.add(new Label("Integer/Store Stations:"), 0, row);
        intField = new TextField(String.valueOf(config.intStations));
        grid.add(intField, 1, row++);

        row++;

        // === Instruction Latencies ===
        grid.add(createSectionLabel("Instruction Latencies (cycles)"), 0, row++, 2, 1);

        grid.add(new Label("ADD/SUB:"), 0, row);
        addSubLatField = new TextField(String.valueOf(config.addSubLatency));
        grid.add(addSubLatField, 1, row++);

        grid.add(new Label("MUL:"), 0, row);
        mulLatField = new TextField(String.valueOf(config.mulLatency));
        grid.add(mulLatField, 1, row++);

        grid.add(new Label("DIV:"), 0, row);
        divLatField = new TextField(String.valueOf(config.divLatency));
        grid.add(divLatField, 1, row++);

        grid.add(new Label("LOAD:"), 0, row);
        loadLatField = new TextField(String.valueOf(config.loadLatency));
        grid.add(loadLatField, 1, row++);

        grid.add(new Label("STORE:"), 0, row);
        storeLatField = new TextField(String.valueOf(config.storeLatency));
        grid.add(storeLatField, 1, row++);

        grid.add(new Label("Integer ALU:"), 0, row);
        intLatField = new TextField(String.valueOf(config.intAluLatency));
        grid.add(intLatField, 1, row++);

        row++;

        // === Cache Configuration ===
        grid.add(createSectionLabel("Cache Configuration"), 0, row++, 2, 1);

        grid.add(new Label("Cache Size (bytes):"), 0, row);
        cacheSizeField = new TextField(String.valueOf(config.cacheSize));
        grid.add(cacheSizeField, 1, row++);

        grid.add(new Label("Block Size (bytes):"), 0, row);
        blockSizeField = new TextField(String.valueOf(config.blockSize));
        grid.add(blockSizeField, 1, row++);

        grid.add(new Label("Cache Hit Latency (cycles):"), 0, row);
        cacheHitField = new TextField(String.valueOf(config.cacheHitLatency));
        grid.add(cacheHitField, 1, row++);

        grid.add(new Label("Cache Miss Penalty (cycles):"), 0, row);
        cacheMissField = new TextField(String.valueOf(config.cacheMissPenalty));
        grid.add(cacheMissField, 1, row++);

        // Buttons
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        
        HBox buttonBox = new HBox(10, okButton, cancelButton);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        grid.add(buttonBox, 0, row, 2, 1);

        // Button actions
        okButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (validateInput()) {
                    okClicked = true;
                    dialogStage.close();
                }
            }
        });

        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                okClicked = false;
                dialogStage.close();
            }
        });

        // Create scene and show
        Scene scene = new Scene(grid);
        dialogStage.setScene(scene);
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-underline: true;");
        return label;
    }

    public boolean showAndWait() {
        dialogStage.showAndWait();
        return okClicked;
    }

    public SimulatorConfig getConfig() {
        return config;
    }

    private boolean validateInput() {
        try {
            config.fpAddStations = Integer.parseInt(fpAddField.getText().trim());
            config.fpMulStations = Integer.parseInt(fpMulField.getText().trim());
            config.loadBuffers = Integer.parseInt(loadField.getText().trim());
            config.intStations = Integer.parseInt(intField.getText().trim());

            config.addSubLatency = Integer.parseInt(addSubLatField.getText().trim());
            config.mulLatency = Integer.parseInt(mulLatField.getText().trim());
            config.divLatency = Integer.parseInt(divLatField.getText().trim());
            config.loadLatency = Integer.parseInt(loadLatField.getText().trim());
            config.storeLatency = Integer.parseInt(storeLatField.getText().trim());
            config.intAluLatency = Integer.parseInt(intLatField.getText().trim());

            config.cacheSize = Integer.parseInt(cacheSizeField.getText().trim());
            config.blockSize = Integer.parseInt(blockSizeField.getText().trim());
            config.cacheHitLatency = Integer.parseInt(cacheHitField.getText().trim());
            config.cacheMissPenalty = Integer.parseInt(cacheMissField.getText().trim());

            return true;
        } catch (NumberFormatException e) {
            // Show error using basic components
            Stage errorStage = new Stage();
            errorStage.setTitle("Invalid Input");
            errorStage.initModality(Modality.WINDOW_MODAL);
            errorStage.initOwner(dialogStage);
            
            VBox errorBox = new VBox(10);
            errorBox.setPadding(new Insets(20));
            errorBox.getChildren().addAll(
                new Label("Please enter valid integer numbers in all fields."),
                new Button("OK") {{
                    setOnAction(ev -> errorStage.close());
                }}
            );
            
            errorStage.setScene(new Scene(errorBox));
            errorStage.showAndWait();
            return false;
        }
    }

    private void copyConfig(SimulatorConfig from, SimulatorConfig to) {
        to.fpAddStations = from.fpAddStations;
        to.fpMulStations = from.fpMulStations;
        to.loadBuffers = from.loadBuffers;
        to.intStations = from.intStations;

        to.addSubLatency = from.addSubLatency;
        to.mulLatency = from.mulLatency;
        to.divLatency = from.divLatency;
        to.loadLatency = from.loadLatency;
        to.storeLatency = from.storeLatency;
        to.intAluLatency = from.intAluLatency;

        to.cacheSize = from.cacheSize;
        to.blockSize = from.blockSize;
        to.cacheHitLatency = from.cacheHitLatency;
        to.cacheMissPenalty = from.cacheMissPenalty;
    }
}