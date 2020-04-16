package dinh.nguyenhuy.ir_remote;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class Section {
    private static Section instance;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference deviceDataRef;
    private DatabaseReference irDataRef;
    private DeviceState deviceState;
    private ArrayList<IRData> irDatas;
    DataSchedule dataSchedules[] = new DataSchedule[10];
    private FirebaseCallbackEvent firebaseCallbackEvent;


    private Section(){

    }

    public static Section getInstance(){
        if(instance == null){
            instance = new Section(FirebaseDatabase.getInstance());
        }
        return instance;
    }

    private Section(final FirebaseDatabase firebaseDatabase){
        this.firebaseDatabase = firebaseDatabase;
        deviceDataRef = firebaseDatabase.getReference("/device");
        irDataRef = firebaseDatabase.getReference("/irdata");
        irDatas = new ArrayList<>();
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

    public String[] getScheduleName() {
        String[] result = new String[10];
        for(int i = 0; i < 10; i++){
            result[i] = dataSchedules[i].getName();
        }
        return result;
    }

    public long[] getScheduleTime(){
        long[] result = new long[10];
        for(int i = 0; i < 10;i++){
            result[i] = dataSchedules[i].getTime();
        }
        return result;
    }

    public int[] getScheduleLoop(){
        int[] result = new int[10];
        for(int i = 0; i < 10;i++){
            result[i] = dataSchedules[i].getLoop();
        }
        return result;
    }

    public void updateDeviceState(DataSnapshot dataSnapshot){
        deviceState = new DeviceState();
        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
            String key = snapshot.getKey();
            if(key.equals("ircode")){
                deviceState.setIrcode(snapshot.getValue(String.class));
            } else if(key.equals("mode")){
                deviceState.setMode(snapshot.getValue(Integer.class));
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

    public void registerCallback(FirebaseCallbackEvent firebaseCallbackEvent){
        this.firebaseCallbackEvent = firebaseCallbackEvent;
    }

    public void removeCallback(){
        firebaseCallbackEvent = null;
    }

    public void sendIR(String ircode){
        deviceDataRef.child("scode").setValue(ircode);
        deviceDataRef.child("mode").setValue(2);
    }

    public boolean sendIR(String name, String ircode, long time, int loop){
        long diff = time - new Date().getTime() / 1000;
        if(diff < 0 || diff > 2592000 || loop < 0 || loop > 24) return false;
        for(int i = 0; i < 10; i++){
            if(dataSchedules[i].getTime() < 10){
                deviceDataRef.child("sname").child("s" + Integer.toString(i)).setValue(name);
                deviceDataRef.child("schedule").child("s" + Integer.toString(i)).setValue(ircode);
                deviceDataRef.child("sloop").child("s" + Integer.toString(i)).setValue(loop);
                deviceDataRef.child("sduration").child("s" + Integer.toString(i)).setValue(time);
                return true;
            }
        }
        return false;
    }

    public void setIRData(String path, String name, String code){
        irDataRef.child(path).child(name).setValue(code);
    }

    public void clearIRBuffer(){
        deviceDataRef.child("ircode").setValue("");
    }

    public void readIR(){
        deviceDataRef.child("mode").setValue(1);
    }

    public void setIdleMode(){
        deviceDataRef.child("mode").setValue(0);
    }


}
