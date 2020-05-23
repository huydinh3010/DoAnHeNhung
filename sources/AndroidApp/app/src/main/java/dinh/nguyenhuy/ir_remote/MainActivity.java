package dinh.nguyenhuy.ir_remote;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listItem;
    Button btnAddDevice;
    Section section;
    ImageButton imgBtn;
    ImageButton speakBtn;
    ArrayList<String> listViewData = new ArrayList<>();
    ArrayAdapter<String> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listItem = findViewById(R.id.list_item);
        adapter = new ArrayAdapter<String>(getApplication(), R.layout.custom_list, R.id.textView, listViewData);
        listItem.setAdapter(adapter);

        btnAddDevice = findViewById(R.id.btnAddDevice);
        imgBtn = findViewById(R.id.imgBtn);
        speakBtn = findViewById(R.id.speak);

        startService(new Intent(this, Section.class));

        //section.registerCallback(this);

        listItem.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                intent.setClass(view.getContext(), SecondActivity.class);
                String[] arrs = listViewData.get(i).split(": ", 2);
                intent.putExtra("name", arrs[1]);
                intent.putExtra("type", arrs[0]);
                startActivity(intent);
            }
        });

        btnAddDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
        imgBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(view.getContext(), ThirdActivity.class);
                startActivity(intent);
            }
        });

        speakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onResume(){
        super.onResume();
        Handler mainHandler = new Handler(getMainLooper());
        final FirebaseCallbackEvent firebaseCallbackEvent = this;
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                try{
                    while(!Section.isCreated) {
                        Thread.sleep(10);
                    }
                    section = Section.getInstance();
                    section.registerCallback(firebaseCallbackEvent);
                    onIRDataChange();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    @Override
    public void onDeviceStatusChange() {

    }

    @Override
    public void onIRDataChange() {
        listViewData.clear();
        for(IRData irData : section.getIrDatas()){
            listViewData.add(irData.getName());
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDataScheduleChange() {

    }


    private void showDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Create new device");
        dialog.setContentView(R.layout.dialog_layout);

        final Spinner spinner = dialog.findViewById(R.id.spinner);
        String[] deviceTypes = {"Air Conditioner", "Tivi", "Fan", "Other"};
        final String[] _types = {"AC", "TV", "FAN", "OTH"};
        spinner.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, deviceTypes));

        dialog.findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String _name = ((EditText)dialog.findViewById(R.id.editText)).getText().toString();
                int pos = spinner.getSelectedItemPosition();
                String _fullname = _types[pos] + ": " + _name;
                if(_name.length() == 0) return;
                section.getIrDatas().add(new IRData(_fullname,new ArrayList<String>(), new ArrayList<String>()));
                Intent intent = new Intent();
                intent.setClass(view.getContext(), SecondActivity.class);
                intent.putExtra("name", _name);
                intent.putExtra("type", _types[pos]);
                Log.e("new device", _fullname);
                startActivity(intent);
                dialog.cancel();
            }
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void promptSpeechInput(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");
        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Sorry! Your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String[] command = (result.get(0) + " .").split(" ");
                String action = command[0];
                String type;
                String name;
                try{
                    if(action.equalsIgnoreCase("turn")){
                        action += " " + command[1];
                        if(action.equalsIgnoreCase("turn on")){
                            type = command[2];
                            if(type.equalsIgnoreCase("tivi") || type.equalsIgnoreCase("tv") || type.equalsIgnoreCase("television")){
                                name = command[3];
                                boolean b = false;
                                String deviceName = "";
                                int offset = -1;
                                for(int i = 0; i < listViewData.size(); i++){
                                    String[] str = listViewData.get(i).split(":");
                                    if(str[0].equals("TV") && str[1].equalsIgnoreCase(name)){
                                        b = true;
                                        deviceName = listViewData.get(i);
                                        break;
                                    }
                                }
                                if(b){
                                    b = false;
                                    ArrayList<String> listBtns = section.getBtnNamesByDevice(deviceName);
                                    for(int i = 0; i < listBtns.size(); i++){
                                        if(listBtns.get(i).equalsIgnoreCase("ON")){
                                            b = true;
                                            offset = i;
                                            break;
                                        }
                                    }
                                }
                                if(b){
                                    if(command[4].equalsIgnoreCase("at")){
                                        String str_time = command[5];

                                    } else{
                                        section.sendIR(section.getIRCodesByDevice(deviceName).get(offset));
                                    }
                                } else{

                                }
                            } else if(type.equalsIgnoreCase("ac") || type.equalsIgnoreCase("air conditioner")){
                                name = command[3];
                                boolean b = false;
                                String deviceName = "";
                                int offset = -1;
                                for(int i = 0; i < listViewData.size(); i++){
                                    String[] str = listViewData.get(i).split(":");
                                    if(str[0].equals("AC") && str[1].equalsIgnoreCase(name)){
                                        b = true;
                                        deviceName = listViewData.get(i);
                                        break;
                                    }
                                }
                                if(b){
                                    b = false;
                                    ArrayList<String> listBtns = section.getBtnNamesByDevice(deviceName);
                                    for(int i = 0; i < listBtns.size(); i++){
                                        if(listBtns.get(i).equalsIgnoreCase("ON")){
                                            b = true;
                                            offset = i;
                                            break;
                                        }
                                    }
                                }
                                if(b){
                                    if(command[4].equalsIgnoreCase("at")){
                                        String str_time = command[5];

                                    } else{
                                        section.sendIR(section.getIRCodesByDevice(deviceName).get(offset));
                                    }
                                } else{

                                }
                            } else if(type.equalsIgnoreCase("fan")){
                                name = command[3];
                                boolean b = false;
                                String deviceName = "";
                                int offset = -1;
                                for(int i = 0; i < listViewData.size(); i++){
                                    String[] str = listViewData.get(i).split(":");
                                    if(str[0].equals("FAN") && str[1].equalsIgnoreCase(name)){
                                        b = true;
                                        deviceName = listViewData.get(i);
                                        break;
                                    }
                                }
                                if(b){
                                    b = false;
                                    ArrayList<String> listBtns = section.getBtnNamesByDevice(deviceName);
                                    for(int i = 0; i < listBtns.size(); i++){
                                        if(listBtns.get(i).equalsIgnoreCase("ON")){
                                            b = true;
                                            offset = i;
                                            break;
                                        }
                                    }
                                }
                                if(b){
                                    if(command[4].equalsIgnoreCase("at")){
                                        String str_time = command[5];

                                    } else{
                                        section.sendIR(section.getIRCodesByDevice(deviceName).get(offset));
                                    }
                                } else{

                                }
                            } else if(!type.equals(".")){
                                name = command[3];
                                boolean b = false;
                                String deviceName = "";
                                int offset = -1;
                                for(int i = 0; i < listViewData.size(); i++){
                                    String[] str = listViewData.get(i).split(":");
                                    if(str[0].equals("OTH") && str[1].equalsIgnoreCase(name)){
                                        b = true;
                                        deviceName = listViewData.get(i);
                                        break;
                                    }
                                }
                                if(b){
                                    b = false;
                                    ArrayList<String> listBtns = section.getBtnNamesByDevice(deviceName);
                                    for(int i = 0; i < listBtns.size(); i++){
                                        if(listBtns.get(i).equalsIgnoreCase("ON")){
                                            b = true;
                                            offset = i;
                                            break;
                                        }
                                    }
                                }
                                if(b){
                                    if(command[4].equalsIgnoreCase("at")){
                                        String str_time = command[5];

                                    } else{
                                        section.sendIR(section.getIRCodesByDevice(deviceName).get(offset));
                                    }
                                } else{

                                }
                            } else{

                            }
                        } else if(action.equalsIgnoreCase("turn off")){
                            type = command[2];
                            if(type.equalsIgnoreCase("tivi") || type.equalsIgnoreCase("tv") || type.equalsIgnoreCase("television")){

                            } else if(type.equalsIgnoreCase("ac") || type.equalsIgnoreCase("air conditioner")){

                            } else if(type.equalsIgnoreCase("fan")){

                            } else if(!type.equals(".")){

                            } else{

                            }
                        } else{

                        }
                    } else if(action.equalsIgnoreCase("increase")){

                    } else if(action.equalsIgnoreCase("decrease")){

                    } else {

                    }
                } catch (Exception e){

                }
            } else{

            }
        }
    }
}
