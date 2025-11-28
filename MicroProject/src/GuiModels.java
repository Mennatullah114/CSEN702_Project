import javafx.beans.property.*;

public class GuiModels {

    public static class RSRow {
        public StringProperty name = new SimpleStringProperty();
        public StringProperty busy = new SimpleStringProperty();
        public StringProperty op = new SimpleStringProperty();
        public StringProperty Vj = new SimpleStringProperty();
        public StringProperty Vk = new SimpleStringProperty();
        public StringProperty Qj = new SimpleStringProperty();
        public StringProperty Qk = new SimpleStringProperty();
        public StringProperty latency = new SimpleStringProperty();

        public RSRow(String name) {
            this.name.set(name);
        }
    }

    public static class ROBRow {
        public StringProperty entry = new SimpleStringProperty();
        public StringProperty dest = new SimpleStringProperty();
        public StringProperty value = new SimpleStringProperty();
        public StringProperty ready = new SimpleStringProperty();

        public ROBRow(int id) {
            this.entry.set("ROB" + id);
        }
    }

    public static class RegRow {
        public StringProperty reg = new SimpleStringProperty();
        public StringProperty value = new SimpleStringProperty();
        public StringProperty tag = new SimpleStringProperty();

        public RegRow(String reg) {
            this.reg.set(reg);
        }
    }
}
