package dinh.nguyenhuy.ir_remote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SecondActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listBtn;
    Button btnAddBtn;
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
            public void onClick(int postion) {
                secsion.sendIR(secsion.getIRCodesByDevice(name).get(postion));
            }
        });
        listBtn.setAdapter(adapter);
        btnAddBtn =findViewById(R.id.btnAddButton);
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
                showDialog();
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

    private void showDialog(){
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
}
