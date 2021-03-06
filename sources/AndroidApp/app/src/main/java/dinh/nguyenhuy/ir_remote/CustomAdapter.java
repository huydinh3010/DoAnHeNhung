package dinh.nguyenhuy.ir_remote;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<String> list;
    private Context context;
    private ButtonListViewEvent buttonEvent;
    private ButtonListViewEvent imgButtonEvent;
    public CustomAdapter(ArrayList<String> list, Context context, ButtonListViewEvent buttonEvent, ButtonListViewEvent imgButtonEvent){
        this.list = list;
        this.context = context;
        this.buttonEvent = buttonEvent;
        this.imgButtonEvent = imgButtonEvent;
    }

    public void setList(ArrayList<String> list){
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.custom_list_2, null);
        }

        //Handle TextView and display string from your list
        TextView textView= (TextView)view.findViewById(R.id.textView);
        textView.setText(list.get(i));

        //Handle buttons and add onClickListeners
        Button callbtn= (Button)view.findViewById(R.id.btn);
        ImageButton imageButton = (ImageButton) view.findViewById(R.id.imgBtn);
        callbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonEvent.onClick(i);
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                imgButtonEvent.onClick(i);
            }
        });

//        view.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                notifyDataSetChanged();
//            }
//        });

        return view;
    }
}
