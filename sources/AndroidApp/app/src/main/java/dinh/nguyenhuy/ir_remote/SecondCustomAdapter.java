package dinh.nguyenhuy.ir_remote;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SecondCustomAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private ArrayList<String> listName;
    private ArrayList<Long> listTime;
    private ArrayList<Integer> listLoop;
    private ArrayList<String> listStatus;

    public SecondCustomAdapter(Context context, ArrayList<String> listName, ArrayList<Long> listTime, ArrayList<Integer> listLoop, ArrayList<String> listStatus) {
        this.context = context;
        this.listName = listName;
        this.listTime = listTime;
        this.listLoop = listLoop;
        this.listStatus = listStatus;
    }

    public void updateData(ArrayList<String> listName, ArrayList<Long> listTime, ArrayList<Integer> listLoop, ArrayList<String> listStatus){
        this.listName = listName;
        this.listTime = listTime;
        this.listLoop = listLoop;
        this.listStatus = listStatus;
    }

    @Override
    public int getCount() {
        return listName.size();
    }

    @Override
    public Object getItem(int i) {
        return listName.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.custom_list_3, null);
        }


        TextView textView = (TextView)view.findViewById(R.id.textView);


        TextView txtDate = (TextView) view.findViewById(R.id.txtDate);
        TextView txtTime = (TextView) view.findViewById(R.id.txtTime);
        TextView txtLoop = (TextView) view.findViewById(R.id.txtLoop);
        TextView txtStatus = (TextView) view.findViewById(R.id.txtStatus);
        Date now = new Date();
        Date date = new Date(listTime.get(i) * 1000L);

        if(listStatus.get(i).equals("WAIT") || listStatus.get(i).equals("DONE") || listStatus.get(i).equals("SET") || listStatus.get(i).equals("OK")){
            textView.setText(listName.get(i));
            txtDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(date));
            txtTime.setText(new SimpleDateFormat("HH:mm").format(date));
            txtLoop.setText("Loop: " + listLoop.get(i) + "h");
            txtStatus.setText(listStatus.get(i));
            txtStatus.setTextColor(0xFF808080);
        } else if(listStatus.get(i).equals("TIME_ERROR") || listStatus.get(i).equals("INVALID_IRCODE") || listStatus.get(i).equals("INVALID_LOOP")){
            textView.setText(listName.get(i));
            txtDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(date));
            txtTime.setText(new SimpleDateFormat("HH:mm").format(date));
            txtLoop.setText("Loop: " + listLoop.get(i) + "h");
            txtStatus.setText(listStatus.get(i));
            txtStatus.setTextColor(0xFFFF0000);
        } else{
            textView.setText("<Empty>");
            txtDate.setText("");
            txtTime.setText("");
            txtLoop.setText("");
            txtStatus.setText("");
            txtStatus.setTextColor(0xFF808080);
        }

//        view.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                notifyDataSetChanged();
//            }
//        });

        return view;
    }
}
