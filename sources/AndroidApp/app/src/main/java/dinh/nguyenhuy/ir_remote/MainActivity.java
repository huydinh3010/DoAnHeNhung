package dinh.nguyenhuy.ir_remote;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listItem;
    Button btnAddDevice;
    Section secsion;
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

        secsion = Section.getInstance();

        secsion.registerCallback(this);

        listItem.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                intent.setClass(view.getContext(), SecondActivity.class);
                intent.putExtra("name", listViewData.get(i));
                startActivity(intent);
            }
        });

        btnAddDevice.setOnClickListener(new View.OnClickListener() {
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
        super.onResume();
        onIRDataChange();
        secsion.registerCallback(this);
    }

    @Override
    public void onDeviceStatusChange() {

    }

    @Override
    public void onIRDataChange() {
        listViewData.clear();
        for(IRData irData : secsion.getIrDatas()){
            listViewData.add(irData.getName());
        }
        adapter.notifyDataSetChanged();
    }

    private void showDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Create new device");
        dialog.setContentView(R.layout.dialog_layout);
        dialog.findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String _name = ((EditText)dialog.findViewById(R.id.editText)).getText().toString();
                if(_name.length() == 0) return;
                secsion.getIrDatas().add(new IRData(_name,new ArrayList<String>(), new ArrayList<String>()));
                Intent intent = new Intent();
                intent.setClass(view.getContext(), SecondActivity.class);
                intent.putExtra("name", _name);
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
}
