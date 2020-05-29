package dinh.nguyenhuy.ir_remote;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SecondActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listBtn;
    Button btnAddBtn;
    ImageButton imgBtn;
    TextView txtName;
    Section section;
    String name;
    String type;
    String fullname;
    ArrayList<String> listViewData = new ArrayList<>();
    CustomAdapter adapter;
    AutoCompleteTextView edtDialog;
    Button btnDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);
        Intent intent = getIntent();
        //section = Section.getInstance();
        //section.registerCallback(this);

        name = intent.getStringExtra("name");
        type = intent.getStringExtra("type");
        if(type == null) type = "OTH";
        fullname = type + ": " + name;

        listBtn = findViewById(R.id.list_btn);
        adapter = new CustomAdapter(listViewData, getApplication(), new ButtonListViewEvent() {
            @Override
            public void onClick(int position) {
                section.sendIR(section.getIRCodesByDevice(fullname).get(position));
            }
        }, new ButtonListViewEvent() {
            @Override
            public void onClick(int position) {
                showDialog3(position);
            }
        });
        listBtn.setAdapter(adapter);
        btnAddBtn =findViewById(R.id.btnAddButton);
        imgBtn = findViewById(R.id.imgBtn);
        txtName = findViewById(R.id.txtName);
        txtName.setText(fullname);

        listBtn.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });

        listBtn.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final int pos = i;
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(SecondActivity.this);
                alertDialog.setTitle("Delete button");
                alertDialog.setMessage("Do you want to delete this button?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        section.removeIR(fullname, section.getBtnNamesByDevice(fullname).get(pos));
                    }
                });

                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                alertDialog.show();
                return true;
            }
        });

        btnAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog2();
            }
        });
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(view.getContext(), ThirdActivity.class);
                startActivity(intent);
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
        try{
            if(section.isCmdChange()){
                if(section.getDeviceState().getCmd().equals("READ_OK")){
                    btnDialog.setText("OK");
                    edtDialog.setVisibility(View.VISIBLE);
                    edtDialog.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            btnDialog.setEnabled(edtDialog.getText().length() > 0);
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {

                        }
                    });
                } else if(section.getDeviceState().getCmd().equals("SEND_ERROR")){
                    Toast.makeText(this, "SEND_ERROR", Toast.LENGTH_LONG).show();
                } else if(section.getDeviceState().getCmd().equals("RESET")){
                    Toast.makeText(this, "Device reset", Toast.LENGTH_LONG).show();
                } else if(section.getDeviceState().getCmd().equals("READ_ERROR")){
                    Toast.makeText(this, "READ_ERROR", Toast.LENGTH_LONG).show();
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onIRDataChange() {
        listViewData.clear();
        for(String btnName : section.getBtnNamesByDevice(fullname)){
            listViewData.add(btnName);
        }
        adapter.setList(listViewData);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDataScheduleChange() {

    }

    private void showDialog2(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout_2);
        btnDialog = dialog.findViewById(R.id.btnRead);
        ArrayList<String> listBtnNames = new ArrayList<>();
        switch (type){
            case "AC":
                listBtnNames.add("ON");
                listBtnNames.add("OFF");
                listBtnNames.add("TEMP +");
                listBtnNames.add("TEMP -");
                break;
            case "TV":
                listBtnNames.add("ON");
                listBtnNames.add("OFF");
                listBtnNames.add("VOL +");
                listBtnNames.add("VOL -");
                break;
            case "FAN":
                listBtnNames.add("ON");
                listBtnNames.add("OFF");
                listBtnNames.add("SPEED +");
                listBtnNames.add("SPEED -");
                break;
            case "OTH":
                listBtnNames.add("ON");
                listBtnNames.add("OFF");
                break;
        }
        edtDialog = dialog.findViewById(R.id.editText);
        edtDialog.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, listBtnNames));
        btnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                section.readIR();
                btnDialog.setEnabled(false);
                btnDialog.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View view1){
                        Log.e("new button", edtDialog.getText().toString());
                        section.setIRData(fullname, edtDialog.getText().toString(), section.getDeviceState().getIrcode());
                        section.clearIRBuffer();
                        dialog.cancel();
                    }
                });
            }
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                section.cancelCmd();
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void showDialog3(final int pos){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout_3);
        final EditText edtDate = dialog.findViewById(R.id.editTextDate);
        final EditText edtTime = dialog.findViewById(R.id.editTextTime);
        final NumberPicker numberPicker = dialog.findViewById(R.id.number_picker);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(24);
        String[] pickerVals = new String[25];
        for(int i = 0; i < 25; i++) pickerVals[i] = i + "h";
        numberPicker.setDisplayedValues(pickerVals);
        edtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(i,i1,i2);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        edtDate.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        edtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edtDate.setTextColor(0xFF808080);
                edtTime.setTextColor(0xFF808080);
                final Calendar calendar = Calendar.getInstance();
                TimePickerDialog timePickerDialog = new TimePickerDialog(view.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        calendar.set(0,0,0, i, i1);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                        edtTime.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }
        });

        dialog.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dateStr = edtDate.getText().toString();
                String timeStr = edtTime.getText().toString();
                if(dateStr.length() == 0){
                    edtDate.setHintTextColor(0xFFFF0000);
                }
                if (timeStr.length() == 0){
                    edtTime.setHintTextColor(0xFFFF0000);
                }
                if (dateStr.length() > 0 && timeStr.length() > 0){
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
                        Date date = dateFormat.parse(dateStr + " " + timeStr);
                        Date now = new Date();
                        long diff = date.getTime() / 1000 - now.getTime() / 1000;
                        int loop = numberPicker.getValue();
                        if(diff < 0 || diff > 2592000){
                            edtDate.setTextColor(0xFFFF0000);
                            edtTime.setTextColor(0xFFFF0000);
                            Toast.makeText(view.getContext(), "Time difference must be greater than 0 and less than 30 days!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String irname = fullname + "/" + section.getBtnNamesByDevice(fullname).get(pos);
                        String ircode = section.getIRCodesByDevice(fullname).get(pos);
                        if(section.sendIR(irname, ircode, date.getTime() / 1000, loop)){
                            dialog.cancel();
                            Intent intent = new Intent();
                            intent.setClass(view.getContext(), ThirdActivity.class);
                            startActivity(intent);
                        }
                    } catch (Exception e){
                        edtDate.setTextColor(0xFFFF0000);
                        edtTime.setTextColor(0xFFFF0000);
                        Toast.makeText(view.getContext(), "Invalid datetime format!", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
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
}
