package dinh.nguyenhuy.ir_remote;

import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ThirdActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listView;
    SecondCustomAdapter adapter;
    Section section;
    ArrayList<String> listName = new ArrayList<>();
    ArrayList<Long> listTime = new ArrayList<>();
    ArrayList<Integer> listLoop = new ArrayList<>();
    ArrayList<String> listStatus = new ArrayList<>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_3);
//        section = Section.getInstance();
//        section.registerCallback(this);


        listView = findViewById(R.id.list);
        adapter = new SecondCustomAdapter(this, listName, listTime, listLoop, listStatus);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                section.removeIRSchedule(i);
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            }
        });
    }

    @Override
    protected void onResume() {
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
                    listName = section.getScheduleName();
                    listTime = section.getScheduleTime();
                    listLoop = section.getScheduleLoop();
                    listStatus = section.getScheduleStatus();
                    section.registerCallback(firebaseCallbackEvent);
                    adapter.updateData(listName, listTime, listLoop, listStatus);
                    adapter.notifyDataSetChanged();
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

    }

    @Override
    public void onDataScheduleChange() {
        listName = section.getScheduleName();
        listTime = section.getScheduleTime();
        listLoop = section.getScheduleLoop();
        listStatus = section.getScheduleStatus();
        adapter.updateData(listName, listTime, listLoop, listStatus);
        adapter.notifyDataSetChanged();
    }
}
