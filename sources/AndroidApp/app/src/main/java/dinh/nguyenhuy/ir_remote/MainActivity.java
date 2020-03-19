package dinh.nguyenhuy.ir_remote;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button btnMode;
    Button btnSendIR;
    Button btnConnect;
    EditText edtIp;

    String ipAddress;
    int mode = 0;
    Socket socket;
    DataOutputStream dos;
    DataInputStream dis;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMode = findViewById(R.id.btnMode);
        btnSendIR = findViewById(R.id.btnSendIR);
        btnConnect = findViewById(R.id.btnConnect);
        edtIp = findViewById(R.id.edtIp);

        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ipAddress = edtIp.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            socket = new Socket(ipAddress, 5000);
                            dos = new DataOutputStream(socket.getOutputStream());
                            dis = new DataInputStream(socket.getInputStream());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(socket != null || socket.isConnected()){
                                        try{
                                            byte data = dis.readByte();
                                            Log.e("wifi_esp", String.valueOf(data));
                                            if(data == '0'){
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        btnMode.setEnabled(true);
                                                        btnMode.setText("Mode 0");
                                                        btnSendIR.setEnabled(true);
                                                    }
                                                });
                                            } else if(data == '1'){
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        btnMode.setEnabled(true);
                                                        btnMode.setText("Mode 1");
                                                        btnSendIR.setEnabled(false);
                                                    }
                                                });
                                            }
                                        } catch (Exception e){
                                            break;
                                        }
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnConnect.setEnabled(true);
                                            btnConnect.setText("Connect");
                                            btnMode.setEnabled(false);
                                            btnSendIR.setEnabled(false);
                                            Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG);
                                        }
                                    });
                                }
                            }).start();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnConnect.setEnabled(false);
                                    btnConnect.setText("Connected!");
                                    Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG);
                                }
                            });

                        } catch (Exception e){
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Can't connect!", Toast.LENGTH_LONG);
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(socket == null || !socket.isConnected()){
                    Toast.makeText(getApplicationContext(), "You must connect first!", Toast.LENGTH_LONG);
                }
                else{
                    btnMode.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                dos.writeChar('0');
                                dos.flush();
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();

                }
            }
        });

        btnSendIR.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(socket == null || !socket.isConnected()){
                    Toast.makeText(getApplicationContext(), "You must connect first!", Toast.LENGTH_LONG);
                }
                else{
                    if(mode == 0){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    dos.writeChar('1');
                                    dos.flush();
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }
        });
    }

}
