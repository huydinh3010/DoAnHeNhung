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

import java.text.SimpleDateFormat;
import java.util.Date;

public class SecondCustomAdapter extends BaseAdapter implements ListAdapter {
    private Context context;
    private String[] listName;
    private long[] listTime;
    private int[] listLoop;

    public SecondCustomAdapter(Context context, String[] listName, long[] listTime, int[] listLoop) {
        this.context = context;
        this.listName = listName;
        this.listTime = listTime;
        this.listLoop = listLoop;
    }

    @Override
    public int getCount() {
        return listName.length;
    }

    @Override
    public Object getItem(int i) {
        return listName[i];
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

        Date now = new Date();
        Date date = new Date(listTime[i] * 1000L);

        if(now.getTime() < date.getTime()){
            textView.setText(listName[i]);
            txtDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(date));
            txtTime.setText(new SimpleDateFormat("HH:mm").format(date));
            txtLoop.setText("Loop: " + listLoop[i] + "h");
        } else{
            textView.setText("<Empty>");
            txtDate.setText("");
            txtTime.setText("");
            txtLoop.setText("");
        }

        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                notifyDataSetChanged();
            }
        });

        return view;
    }
}
