import javafx.beans.property.*;

public class GuiModels {

    public static class RSRow {
        private StringProperty name = new SimpleStringProperty();
        private StringProperty busy = new SimpleStringProperty();
        private StringProperty op = new SimpleStringProperty();
        private StringProperty Vj = new SimpleStringProperty();
        private StringProperty Vk = new SimpleStringProperty();
        private StringProperty Qj = new SimpleStringProperty();
        private StringProperty Qk = new SimpleStringProperty();
        private StringProperty latency = new SimpleStringProperty();

        public ReservationStation rs;

        public RSRow(ReservationStation rs) {
            this.rs = rs;
            this.name.set(rs.name);
        }

        // Getters for PropertyValueFactory
        public StringProperty nameProperty() { return name; }
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }

        public StringProperty busyProperty() { return busy; }
        public String getBusy() { return busy.get(); }
        public void setBusy(String value) { busy.set(value); }

        public StringProperty opProperty() { return op; }
        public String getOp() { return op.get(); }
        public void setOp(String value) { op.set(value); }

        public StringProperty VjProperty() { return Vj; }
        public String getVj() { return Vj.get(); }
        public void setVj(String value) { Vj.set(value); }

        public StringProperty VkProperty() { return Vk; }
        public String getVk() { return Vk.get(); }
        public void setVk(String value) { Vk.set(value); }

        public StringProperty QjProperty() { return Qj; }
        public String getQj() { return Qj.get(); }
        public void setQj(String value) { Qj.set(value); }

        public StringProperty QkProperty() { return Qk; }
        public String getQk() { return Qk.get(); }
        public void setQk(String value) { Qk.set(value); }

        public StringProperty latencyProperty() { return latency; }
        public String getLatency() { return latency.get(); }
        public void setLatency(String value) { latency.set(value); }
    }

    public static class ROBRow {
        private StringProperty entry = new SimpleStringProperty();
        private StringProperty dest = new SimpleStringProperty();
        private StringProperty value = new SimpleStringProperty();
        private StringProperty ready = new SimpleStringProperty();

        public ROBRow(int id) {
            this.entry.set("ROB" + id);
        }

        // Getters for PropertyValueFactory
        public StringProperty entryProperty() { return entry; }
        public String getEntry() { return entry.get(); }
        public void setEntry(String value) { entry.set(value); }

        public StringProperty destProperty() { return dest; }
        public String getDest() { return dest.get(); }
        public void setDest(String value) { dest.set(value); }

        public StringProperty valueProperty() { return value; }
        public String getValue() { return value.get(); }
        public void setValue(String val) { value.set(val); }

        public StringProperty readyProperty() { return ready; }
        public String getReady() { return ready.get(); }
        public void setReady(String value) { ready.set(value); }
    }

    public static class RegRow {
        private StringProperty reg = new SimpleStringProperty();
        private StringProperty value = new SimpleStringProperty();
        private StringProperty tag = new SimpleStringProperty();

        public RegRow(String reg) {
            this.reg.set(reg);
        }

        // Getters for PropertyValueFactory
        public StringProperty regProperty() { return reg; }
        public String getReg() { return reg.get(); }
        public void setReg(String value) { reg.set(value); }

        public StringProperty valueProperty() { return value; }
        public String getValue() { return value.get(); }
        public void setValue(String val) { value.set(val); }

        public StringProperty tagProperty() { return tag; }
        public String getTag() { return tag.get(); }
        public void setTag(String value) { tag.set(value); }
    }
}