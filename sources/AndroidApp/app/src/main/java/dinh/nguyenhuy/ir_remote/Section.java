package dinh.nguyenhuy.ir_remote;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Section extends Service {
    public static boolean isCreated = false;
    private static Section instance;
    private Context context;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference deviceDataRef;
    private DatabaseReference irDataRef;
    private DeviceState deviceState;
    private ArrayList<IRData> irDatas;
    private boolean cmdChange;
    private NotificationManager notificationManager;
    DataSchedule dataSchedules[] = new DataSchedule[10];
    private FirebaseCallbackEvent firebaseCallbackEvent;


    public Section(){
        super();
    }

    public static Section getInstance(){

        return instance;
    }

    private void setup(){
        this.firebaseDatabase = FirebaseDatabase.getInstance();
        deviceDataRef = firebaseDatabase.getReference("/device");
        irDataRef = firebaseDatabase.getReference("/irdata");
        irDatas = new ArrayList<>();
        deviceState = new DeviceState();
        for(int i = 0; i < 10; i++) dataSchedules[i] = new DataSchedule(i);
        deviceDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Firebase", "onDeviceStatusChange");
                updateDeviceState(dataSnapshot);
                if(firebaseCallbackEvent != null){
                    firebaseCallbackEvent.onDeviceStatusChange();
                    firebaseCallbackEvent.onDataScheduleChange();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException());
            }
        });


        irDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("Firebase", "onIRDataChange");
                updateIRData(dataSnapshot);
                if(firebaseCallbackEvent != null) firebaseCallbackEvent.onIRDataChange();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException());
            }
        });
    }

    private Section(final FirebaseDatabase firebaseDatabase){
        this.firebaseDatabase = firebaseDatabase;
        deviceDataRef = firebaseDatabase.getReference("/device");
        irDataRef = firebaseDatabase.getReference("/irdata");
        irDatas = new ArrayList<>();
        deviceState = new DeviceState();
        for(int i = 0; i < 10; i++) dataSchedules[i] = new DataSchedule(i);
        deviceDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateDeviceState(dataSnapshot);
                if(firebaseCallbackEvent != null){
                    firebaseCallbackEvent.onDeviceStatusChange();
                    firebaseCallbackEvent.onDataScheduleChange();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException());
            }
        });


        irDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateIRData(dataSnapshot);
                if(firebaseCallbackEvent != null) firebaseCallbackEvent.onIRDataChange();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read value.", databaseError.toException());
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }



    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        instance = this;

        setup();
        //getInstance();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        isCreated = true;
        Log.e("service", "onCreate");
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("dinh.nguyenhuy.ServiceStopped");
//        registerReceiver(mIntentReceiver, intentFilter);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("service", "onDestroy");
//        Intent intent = new Intent("dinh.nguyenhuy.ServiceStopped");
//        sendBroadcast(intent);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startService(new Intent(context, Section.class));
//        } else {
//            context.startService(new Intent(context, Section.class));
//        }
    }

//    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String mAction = intent.getAction();
//            Log.e("service", "onReceive: " + mAction);
//            if (mAction.equals("dinh.nguyenhuy.ServiceStopped")) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(new Intent(context, Section.class));
//                } else {
//                    context.startService(new Intent(context, Section.class));
//                }
//                Log.e("service", "onReceive");
//
//            }
//        }
//    };

    public ArrayList<String> getScheduleName() {
        ArrayList<String> result = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            result.add(dataSchedules[i].getName());
        }
        return result;
    }

    public ArrayList<Long> getScheduleTime(){
        ArrayList<Long> result = new ArrayList<>();
        for(int i = 0; i < 10;i++){
            result.add(dataSchedules[i].getTime());
        }
        return result;
    }

    public ArrayList<Integer> getScheduleLoop(){
        ArrayList<Integer> result = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            result.add(dataSchedules[i].getLoop());
        }
        return result;
    }

    public ArrayList<String> getScheduleStatus(){
        ArrayList<String> result = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            result.add(dataSchedules[i].getStatus());
        }
        return result;
    }


    public void updateDeviceState(DataSnapshot dataSnapshot){
        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
            String key = snapshot.getKey();
            if(key.equals("ircode")){
                deviceState.setIrcode(snapshot.getValue(String.class));
            } else if(key.equals("cmd")){
                String cmd = snapshot.getValue(String.class);
                cmdChange = !cmd.equals(deviceState.getCmd());
                deviceState.setCmd(cmd);
                if(cmd.equals("RESET")){
                    deviceDataRef.child("cmd").setValue("CANCEL");
                    if(notificationManager == null) continue;
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "channel_id")
                            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                            .setContentTitle("There was an error with ESP-8266")
                            .setContentText("ESP-8266 has restarted")
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        String channelId = "channel_id";
                        NotificationChannel channel = new NotificationChannel(
                                channelId,
                                "Channel human readable title",
                                NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }
                    notificationManager.notify(10, mBuilder.build());
                }
            } else if(key.equals("scode")){
                deviceState.setScode(snapshot.getValue(String.class));
            } else if(key.equals("schedule")){
                int i = 0;
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    if(i < 10){
                        dataSchedules[i].setData(snapshot1.getValue(String.class));
                    }
                    i++;
                }
            } else if(key.equals("sduration")){
                int i = 0;
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    if(i < 10){
                        dataSchedules[i].setTime(snapshot1.getValue(Long.class));
                    }
                    i++;
                }
            } else if(key.equals("sname")){
                int i = 0;
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    if(i < 10){
                        dataSchedules[i].setName(snapshot1.getValue(String.class));
                    }
                    i++;
                }
            } else if(key.equals("sloop")){
                int i = 0;
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    if(i < 10){
                        dataSchedules[i].setLoop(snapshot1.getValue(Integer.class));
                    }
                    i++;
                }
            } else if(key.equals("sstatus")){
                int i = 0;
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    if(i < 10){
                        String _status = snapshot1.getValue(String.class);
                        dataSchedules[i].setStatus(_status);
                        if(_status.equals("DONE")){
                            deviceDataRef.child("sstatus").child("s" + i).setValue("OK");
                            Log.e("notification", "show");
                            if(notificationManager == null) continue;
                            Intent intent = new Intent(context, ThirdActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "channel_id")
                                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                                    .setContentTitle("IRCode sent successfully: " + dataSchedules[i].getName())
                                    .setContentText("Sent at: " + new SimpleDateFormat("HH:mm").format(new Date(dataSchedules[i].getTime() * 1000L)))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);


                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            {
                                String channelId = "channel_id";
                                NotificationChannel channel = new NotificationChannel(
                                        channelId,
                                        "Channel human readable title",
                                        NotificationManager.IMPORTANCE_HIGH);
                                notificationManager.createNotificationChannel(channel);
                            }
                            notificationManager.notify(i, mBuilder.build());
                        }
                    }
                    i++;
                }
            }
        }
    }

    public void updateIRData(DataSnapshot dataSnapshot){
        irDatas = new ArrayList<>();
        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
            String key = snapshot.getKey();
            ArrayList<String> keys = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            for(DataSnapshot snapshot1 : snapshot.getChildren()){
                keys.add(snapshot1.getKey());
                values.add(snapshot1.getValue(String.class));
            }
            irDatas.add(new IRData(key, keys, values));
        }
    }

    public ArrayList<IRData> getIrDatas() {
        return irDatas;
    }

    public ArrayList<String> getIRCodesByDevice(String name){
        for(IRData irData : irDatas){
            if(irData.getName().equals(name)){
                return irData.getIrCodes();
            }
        }
        return new ArrayList<>();
    }

    public ArrayList<String> getBtnNamesByDevice(String name){
        for(IRData irData : irDatas){
            if(irData.getName().equals(name)){
                return irData.getButtonNames();
            }
        }
        return new ArrayList<>();
    }

    public DeviceState getDeviceState() {
        return deviceState;
    }

    public boolean isCmdChange(){
        return cmdChange;
    }

    public void registerCallback(FirebaseCallbackEvent firebaseCallbackEvent){
        this.firebaseCallbackEvent = firebaseCallbackEvent;
    }

    public void removeCallback(){
        firebaseCallbackEvent = null;
    }

    public void sendIR(String ircode){
        deviceDataRef.child("scode").setValue(ircode);
        if(deviceState.getCmd().equals("SEND")) deviceDataRef.child("cmd").setValue("");
        deviceDataRef.child("cmd").setValue("SEND");
    }

    public boolean sendIR(String name, String ircode, long time, int loop){
        long diff = time - new Date().getTime() / 1000;
        if(diff < 0 || diff > 2592000 || loop < 0 || loop > 24) return false;
        for(int i = 0; i < 10; i++){
            if(!dataSchedules[i].getStatus().equals("WAIT") && !dataSchedules[i].getStatus().equals("SET")){
                deviceDataRef.child("sname").child("s" + i).setValue(name);
                deviceDataRef.child("schedule").child("s" + i).setValue(ircode);
                deviceDataRef.child("sloop").child("s" + i).setValue(loop);
                deviceDataRef.child("sduration").child("s" + i).setValue(time);
                if(dataSchedules[i].getStatus().equals("SET")) deviceDataRef.child("sstatus").child("s" + i).setValue("");
                deviceDataRef.child("sstatus").child("s" + i).setValue("SET");
                return true;
            }
        }
        return false;
    }

    public void removeIRSchedule(int pos){
        deviceDataRef.child("sstatus").child("s" + pos).setValue("CANCEL");
    }

    public void removeIR(String path, String name){
        irDataRef.child(path).child(name).removeValue();
    }

    public void setIRData(String path, String name, String code){
        irDataRef.child(path).child(name).setValue(code);
    }

    public void clearIRBuffer(){
        deviceDataRef.child("ircode").setValue("");
    }

    public void readIR(){
//        actionCmd = "READ";
//        waitCmd = true;
        if(deviceState.getCmd().equals("READ")) deviceDataRef.child("cmd").setValue("");
        deviceDataRef.child("cmd").setValue("READ");
    }

    public void cancelCmd(){
        deviceDataRef.child("cmd").setValue("CANCEL");
    }

}
