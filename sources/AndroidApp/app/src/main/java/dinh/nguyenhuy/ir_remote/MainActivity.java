package dinh.nguyenhuy.ir_remote;


import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
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
            if (resultCode != RESULT_OK || null == data || !validateCommand(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS))) {
                Toast.makeText(this, "The command is invalid or not supported", Toast.LENGTH_LONG).show();
            }
        }

    }

    private boolean validateCommand(ArrayList<String> result){
        String command = result.get(0).trim().toLowerCase();
        String regex1 = "turn (on|off) (the|)(|.*) (air conditioner|ac|tv|tivi|television|fan|device)";
        String regex2 = "turn (on|off) (the|)(|.*) (air conditioner|ac|tv|tivi|television|fan|device) at ([0-9]|1[0-9]|2[0-3]) (pm|am|o'clock|)( tomorrow|)";
        String regex3 = "turn (on|off) (the|)(|.*) (air conditioner|ac|tv|tivi|television|fan|device) after ([1-9]|1[0-9]|2[0-4]) (hours|hour)";

        //pre_process
        String[] str_arr = command.split(" ");
        String w_num[] = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twenty one", "twenty two", "twenty three", "twenty four"};
        command = "";
        for(int i = 0; i < str_arr.length; i++){
            for(int j = 0; j < w_num.length; j++){
                if(str_arr[i].equals(w_num[j])){
                    str_arr[i] = Integer.toString(j);
                    break;
                }
            }
            command += str_arr[i] + " ";
        }
        command = command.trim();
        //
        String _action;
        String _name;
        String _type;
        Date _time = null;
        Matcher matcher = Pattern.compile(regex1).matcher(command);
        if(matcher.matches()){
            _action = matcher.group(1).trim();
            _name = matcher.group(3).trim();
            _type = matcher.group(4).trim();
        } else {
            matcher = Pattern.compile(regex2).matcher(command);
            if(matcher.matches()){
                _action = matcher.group(1).trim();
                _name = matcher.group(3).trim();
                _type = matcher.group(4).trim();
                int _hour = Integer.parseInt(matcher.group(5).trim());
                String t_format = matcher.group(6);
                if(t_format.equals("pm") && _hour <= 12) _hour += 12;
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, _hour);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                if(matcher.group(7).trim().equals("tomorrow")){
                    c.add(Calendar.DAY_OF_MONTH, 1);
                }
                _time = c.getTime();
            } else{
                matcher = Pattern.compile(regex3).matcher(command);
                if(matcher.matches()){
                    _action = matcher.group(1).trim();
                    _name = matcher.group(3).trim();
                    _type = matcher.group(4).trim();
                    int _hour = Integer.parseInt(matcher.group(5).trim());
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.HOUR_OF_DAY, _hour);
                    _time = c.getTime();
                } else{
                    // fail
                    return false;
                }
            }
        }
        if(_time != null){
            long diff = _time.getTime() / 1000 - new Date().getTime() / 1000;
            if(diff < 0 || diff > 2592000){
                // fail
                return false;
            }
        }
        if(_type.equals("tivi") || _type.equals("tv") || _type.equals("television")) _type = "tv";
        else if(_type.equals("air conditioner") || _type.equals("ac")) _type = "ac";
        else if(_type.equals("fan")) _type = "fan";
        else if(_type.equals("device")) _type = "device";
        final ArrayList<String> fullnames = new ArrayList<>();
        final ArrayList<String> irCodes = new ArrayList<>();
        final long _ltime = _time == null ? 0 : _time.getTime()/1000;
        final String f_action = _action.toUpperCase();
        for(int i = 0; i < listViewData.size(); i++){
            String[] str = listViewData.get(i).split(":");
            str[0] = str[0].trim();
            str[1] = str[1].trim();
            if((str[0].equalsIgnoreCase(_type) || _type.equals("device")) && (str[1].equalsIgnoreCase(_name) || _name.equals(""))){
                ArrayList<String> listBtn = section.getBtnNamesByDevice(listViewData.get(i));
                for(int j = 0; j < listBtn.size(); j++){
                    if(listBtn.get(j).equalsIgnoreCase(_action)){
                        fullnames.add(listViewData.get(i));
                        irCodes.add(section.getIRCodesByDevice(listViewData.get(i)).get(j));
                    }
                }
            }
        }
        if(irCodes.size() == 1) {
            if(_ltime != 0){
                if(section.sendIR(fullnames.get(0)+"/"+f_action, irCodes.get(0),_ltime ,0)){
                    Toast.makeText(this, "The command is executed", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent();
                    intent.setClass(this, ThirdActivity.class);
                    startActivity(intent);
                } else{
                    Toast.makeText(MainActivity.this, "Slots is full, cannot setup timer", Toast.LENGTH_LONG).show();
                }
            } else{
                section.sendIR(irCodes.get(0));
                Toast.makeText(this, "The command is executed", Toast.LENGTH_LONG).show();
            }
        } else if(irCodes.size() > 1) {
            // show list
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
            builderSingle.setTitle("Select device");

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            arrayAdapter.addAll(fullnames);

            builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int pos) {
                    if(_ltime != 0){
                        if(section.sendIR(fullnames.get(pos)+"/"+f_action, irCodes.get(pos),_ltime ,0)){
                            Toast.makeText(MainActivity.this, "The command is executed", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent();
                            intent.setClass(getApplicationContext(), ThirdActivity.class);
                            startActivity(intent);
                        } else{
                            Toast.makeText(MainActivity.this, "Slots is full, cannot setup timer", Toast.LENGTH_LONG).show();
                        }
                    } else{
                        Toast.makeText(MainActivity.this, "The command is executed", Toast.LENGTH_LONG).show();
                        section.sendIR(irCodes.get(pos));
                    }
                }
            });
            builderSingle.show();
        } else{
            // fail;
            return false;
        }
        return true;
    }
}
