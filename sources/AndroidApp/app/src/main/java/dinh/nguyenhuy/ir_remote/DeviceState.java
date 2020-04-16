package dinh.nguyenhuy.ir_remote;

public class DeviceState {
    private String ircode;
    private int mode;
    private String scode;

    public DeviceState(){

    }

    public DeviceState(String ircode, int mode, String scode) {
        this.ircode = ircode;
        this.mode = mode;
        this.scode = scode;
    }

    public String getIrcode() {
        return ircode;
    }

    public void setIrcode(String ircode) {
        this.ircode = ircode;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getScode() {
        return scode;
    }

    public void setScode(String scode) {
        this.scode = scode;
    }
}
