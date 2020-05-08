package dinh.nguyenhuy.ir_remote;

public class DeviceState {
    private String ircode;
    private String cmd;
    private String scode;

    public DeviceState(){

    }

    public DeviceState(String ircode, String cmd, String scode) {
        this.ircode = ircode;
        this.cmd = cmd;
        this.scode = scode;
    }

    public String getIrcode() {
        return ircode;
    }

    public void setIrcode(String ircode) {
        this.ircode = ircode;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getScode() {
        return scode;
    }

    public void setScode(String scode) {
        this.scode = scode;
    }
}
