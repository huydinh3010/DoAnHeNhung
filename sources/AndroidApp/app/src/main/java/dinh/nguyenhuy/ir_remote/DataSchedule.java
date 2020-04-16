package dinh.nguyenhuy.ir_remote;

public class DataSchedule {
    private String name;
    private String data;
    private long time;
    private int loop;
    private int pos;

    public DataSchedule(int pos, String name, String data, long time, int loop) {
        this.pos = pos;
        this.name = name;
        this.data = data;
        this.time = time;
        this.loop = loop;
    }

    public DataSchedule(int pos) {
        this.pos = pos;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }
}
