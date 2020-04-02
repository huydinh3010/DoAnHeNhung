package dinh.nguyenhuy.ir_remote;

import java.util.ArrayList;

public class IRData {
    private String name;
    private ArrayList<String> buttonNames;
    private ArrayList<String> irCodes;

    public IRData(){

    }

    public IRData(String name, ArrayList<String> buttonNames, ArrayList<String> irCodes) {
        this.name = name;
        this.buttonNames = buttonNames;
        this.irCodes = irCodes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getButtonNames() {
        return buttonNames;
    }

    public void setButtonNames(ArrayList<String> buttonNames) {
        this.buttonNames = buttonNames;
    }

    public ArrayList<String> getIrCodes() {
        return irCodes;
    }

    public void setIrCodes(ArrayList<String> irCodes) {
        this.irCodes = irCodes;
    }

    public String toString(){
        String s = name;
        for(int i = 0; i < buttonNames.size(); i++){
            s += "{" + buttonNames.get(i) + ", " + irCodes.get(i) + "}, ";
        }
        return s;
    }
}
