package dinh.nguyenhuy.ir_remote;

public class DeviceState {
    private String ircode;
    private int mode;
    private String scode;
    private String time;

    public DeviceState(){

    }

    public DeviceState(String ircode, int mode, String scode, String time) {
        this.ircode = ircode;
        this.mode = mode;
        this.scode = scode;
        this.time = time;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String toString(){
        return "IRCode: " + ircode + ", mode: " + mode + ", Scode: " + scode + ", time: " + time;
    }
}
