package dinh.nguyenhuy.ir_remote;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Section {
    private static Section instance;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference deviceDataRef;
    private DatabaseReference irDataRef;
    private DeviceState deviceState;
    private ArrayList<IRData> irDatas;
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
        deviceDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                updateDeviceState(dataSnapshot);
                if(firebaseCallbackEvent != null) firebaseCallbackEvent.onDeviceStatusChange();
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

    public void updateDeviceState(DataSnapshot dataSnapshot){
        deviceState = dataSnapshot.getValue(DeviceState.class);
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

    public void sendIR(String ircode, String time){
        deviceDataRef.child("scode").setValue(ircode);
        deviceDataRef.child("mode").setValue(2);
        deviceDataRef.child("time").setValue(time);

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
