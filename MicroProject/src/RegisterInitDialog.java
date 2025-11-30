import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.stage.*;
import java.util.*;

public class RegisterInitDialog {
    
    private Stage dialogStage;
    private boolean okClicked = false;
    private RegisterFile registers;
    
    private Map<String, TextField> fields = new HashMap<>();
    
    public RegisterInitDialog(Stage owner, RegisterFile registers) {
        this.registers = registers;
        
        dialogStage = new Stage();
        dialogStage.setTitle("Initialize Registers");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setResizable(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20));
        
        Label headerLabel = new Label("Set initial register values");
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(headerLabel, 0, 0, 4, 1);
        
        // Create fields for commonly used registers
        String[] commonRegs = {"R0", "R1", "R2", "R3", "R4", "F0", "F1", "F2", "F3", "F4", "F6"};
        
        int row = 1;
        for (String reg : commonRegs) {
            Label label = new Label(reg + ":");
            TextField field = new TextField("0");
            field.setPrefWidth(80);
            fields.put(reg, field);
            
            int col = (row - 1) % 2;
            int actualRow = 1 + (row - 1) / 2;
            
            grid.add(label, col * 2, actualRow);
            grid.add(field, col * 2 + 1, actualRow);
            row++;
        }
        
        // Buttons
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        
        okButton.setPrefWidth(80);
        cancelButton.setPrefWidth(80);
        
        HBox buttonBox = new HBox(10, okButton, cancelButton);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        grid.add(buttonBox, 0, 10, 4, 1);
        
        okButton.setOnAction(e -> {
            if (applyValues()) {
                okClicked = true;
                dialogStage.close();
            }
        });
        
        cancelButton.setOnAction(e -> {
            okClicked = false;
            dialogStage.close();
        });
        
        Scene scene = new Scene(grid);
        dialogStage.setScene(scene);
    }
    
    public boolean showAndWait() {
        dialogStage.showAndWait();
        return okClicked;
    }
    
    private boolean applyValues() {
        try {
            for (Map.Entry<String, TextField> entry : fields.entrySet()) {
                String regName = entry.getKey();
                String text = entry.getValue().getText().trim();
                
                RegisterFile.Register reg = registers.get(regName);
                if (reg != null) {
                    if (regName.startsWith("R")) {
                        // Integer register - only accept integers
                        int intValue = Integer.parseInt(text);
                        reg.value = intValue;
                    } else if (regName.startsWith("F")) {
                        // Floating point register - accept floating point
                        double fpValue = Double.parseDouble(text);
                        reg.value = fpValue;
                    }
                }
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers:\n- Integer registers (R): whole numbers only\n- Floating point registers (F): decimal numbers allowed");
            return false;
        }
    }
    
    private void showError(String message) {
        Stage errorStage = new Stage();
        errorStage.setTitle("Invalid Input");
        errorStage.initModality(Modality.WINDOW_MODAL);
        errorStage.initOwner(dialogStage);
        
        VBox errorBox = new VBox(15);
        errorBox.setPadding(new Insets(20));
        errorBox.setStyle("-fx-alignment: center;");
        
        Label errorLabel = new Label(message);
        Button okBtn = new Button("OK");
        okBtn.setPrefWidth(80);
        okBtn.setOnAction(ev -> errorStage.close());
        
        errorBox.getChildren().addAll(errorLabel, okBtn);
        
        errorStage.setScene(new Scene(errorBox, 300, 100));
        errorStage.showAndWait();
    }
}