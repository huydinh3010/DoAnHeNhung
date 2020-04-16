package dinh.nguyenhuy.ir_remote;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
    Section secsion;
    String name;
    ArrayList<String> listViewData = new ArrayList<>();
    CustomAdapter adapter;
    int state = 0;
    EditText edtDialog;
    Button btnDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);
        Intent intent = getIntent();
        secsion = Section.getInstance();
        secsion.registerCallback(this);

        name = intent.getStringExtra("name");

        for(String btnName : secsion.getBtnNamesByDevice(name)){
            listViewData.add(btnName);
        }

        listBtn = findViewById(R.id.list_btn);
        adapter = new CustomAdapter(listViewData, getApplication(), new ButtonListViewEvent() {
            @Override
            public void onClick(int position) {
                secsion.sendIR(secsion.getIRCodesByDevice(name).get(position));
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
        txtName.setText(name);

        listBtn.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

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
        state = 0;
        super.onResume();
        onIRDataChange();
        secsion.registerCallback(this);
    }

    @Override
    public void onDeviceStatusChange() {
        try{
            if(secsion.getDeviceState().getIrcode().length() > 0 && secsion.getDeviceState().getMode() == 0){
                btnDialog.setText("OK");
                btnDialog.setEnabled(true);
                edtDialog.setVisibility(View.VISIBLE);
                state = 1;
            } else{
                state = 0;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onIRDataChange() {
        listViewData.clear();
        for(String btnName : secsion.getBtnNamesByDevice(name)){
            listViewData.add(btnName);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDataScheduleChange() {

    }

    private void showDialog2(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_layout_2);
        edtDialog = dialog.findViewById(R.id.editText);
        btnDialog = dialog.findViewById(R.id.btnRead);
        btnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(state == 0){
                    secsion.readIR();
                    btnDialog.setEnabled(false);
                } else if(state == 1){
                    if(edtDialog.getText().toString().length() == 0) return;
                    secsion.setIRData(name, edtDialog.getText().toString(), secsion.getDeviceState().getIrcode());
                    secsion.clearIRBuffer();
                    dialog.cancel();
                }
            }
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                secsion.setIdleMode();
                state = 0;
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
                        String irname = name + "/" + secsion.getBtnNamesByDevice(name).get(pos);
                        String ircode = secsion.getIRCodesByDevice(name).get(pos);
                        if(secsion.sendIR(irname, ircode, date.getTime() / 1000, loop)){
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
