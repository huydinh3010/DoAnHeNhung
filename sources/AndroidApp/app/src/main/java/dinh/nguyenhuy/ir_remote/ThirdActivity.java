package dinh.nguyenhuy.ir_remote;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class ThirdActivity extends AppCompatActivity implements FirebaseCallbackEvent{
    ListView listView;
    SecondCustomAdapter adapter;
    Section section;
    String listName[];
    long listTime[];
    int listLoop[];
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_3);
        section = Section.getInstance();
        section.registerCallback(this);
        listName = section.getScheduleName();
        listTime = section.getScheduleTime();
        listLoop = section.getScheduleLoop();

        listView = findViewById(R.id.list);
        adapter = new SecondCustomAdapter(this, listName, listTime, listLoop);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        section.registerCallback(this);
        adapter.notifyDataSetChanged();
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
        adapter.notifyDataSetChanged();
    }
}
