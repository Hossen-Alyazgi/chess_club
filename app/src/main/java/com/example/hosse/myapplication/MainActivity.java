package com.example.hosse.myapplication;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button  btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box,connectionStatus,opponentName;
    EditText writeMsg;

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ = 1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    Dialog connectDialog;
    ImageView closeConnectPopupImg;
    Button btnyes;
    Dialog versusDialog;
    Dialog resultDialog;

    boolean isCredential;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "text";

    private String username;
    Dialog getNameDialog;

    private String sentMessage;



    public void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();



        editor.apply();

    }
    public void loadData(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        username = sharedPreferences.getString(TEXT,"");


    }
    public void UpdateViews(){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
        openNameDialog();


    }

    String tempMsg;
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    if(isCredential){
                        byte[] readBuff = (byte[]) msg.obj;
                        tempMsg = new String(readBuff, 0, msg.arg1);
                        ((TextView)versusDialog.findViewById(R.id.opponentName)).setText(tempMsg);
                        break;

                    }else{
                        byte[] readBuff = (byte[]) msg.obj;
                        tempMsg = new String(readBuff, 0, msg.arg1);
                        read_msg_box.setText(tempMsg);
                        break;
                    }

            }
            return true;
        }
    });

    private void exqListener() {
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }
                    @Override
                    public void onFailure(int reason) {
                        connectionStatus.setText("Discovery failed to start");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                final WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                connectDialog.setContentView(R.layout.popup_connect);
                closeConnectPopupImg = connectDialog.findViewById(R.id.closePopupConnect);
                btnyes = connectDialog.findViewById(R.id.btnaccept);
                Objects.requireNonNull(connectDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                connectDialog.show();
                closeConnectPopupImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectDialog.dismiss();
                    }
                });

                btnyes.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mManager.connect(mChannel,config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(), "connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
                            }
                        });

                        connectDialog.dismiss();

                    }
                });

            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MultiTask().execute();
            }

        });
    }
    public void openResultDialog(){
        resultDialog.setContentView(R.layout.result_dialog);
        Objects.requireNonNull(resultDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        ((ImageView) resultDialog.findViewById(R.id.closePopupConnect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versusDialog.dismiss();
            }
        });
        ((Button) resultDialog.findViewById(R.id.opponnentbtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultDialog.dismiss();
            }
        });
        ((Button) resultDialog.findViewById(R.id.mebtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultDialog.dismiss();
            }
        });
        resultDialog.show();

    }
    public void openVersusDialog(){
        versusDialog.setContentView(R.layout.versus_popup);
        Objects.requireNonNull(versusDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ((TextView) versusDialog.findViewById(R.id.YourName)).setText(username);
        ImageView closeVersusPopupImg = versusDialog.findViewById(R.id.closeVersusPopup);
        Button finishButton = (Button) versusDialog.findViewById(R.id.finish_button);

        closeVersusPopupImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versusDialog.dismiss();
            }
        });
        Objects.requireNonNull(versusDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versusDialog.dismiss();
                openResultDialog();
            }
        });
        versusDialog.show();

    }
    public void setMessage(String message){
        sentMessage = message;

    }
    public String getMessage(){
        return sentMessage;
    }

    private void initialWork() {


        btnDiscover = findViewById(R.id.discover);
        listView = findViewById(R.id.peerListView);
        read_msg_box = findViewById(R.id.readMsg);

        connectionStatus = findViewById(R.id.connectionStatus);


        btnSend = findViewById(R.id.sendButton);
        writeMsg = findViewById(R.id.writeMsg);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        connectDialog = new Dialog(this);

        versusDialog = new Dialog(this);
        opponentName = versusDialog.findViewById(R.id.opponentName);

        getNameDialog = new Dialog(this);

        resultDialog = new Dialog(this);
    }
    public void openNameDialog(){

        getNameDialog.setContentView(R.layout.name_input);
        Objects.requireNonNull(getNameDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        getNameDialog.findViewById(R.id.closeNamePopup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNameDialog.dismiss();
            }
        });
        getNameDialog.findViewById(R.id.donenamepopup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versusDialog.dismiss();
                username = ((TextView) getNameDialog.findViewById(R.id.writeName)).getText().toString();
                Toast.makeText(getApplicationContext(), "your name is "+username, Toast.LENGTH_SHORT).show();
                getNameDialog.dismiss();


            }
        });
        getNameDialog.show();

    }
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Devices Found", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable( final WifiP2pInfo wifiP2pinfo) {
            final InetAddress groupOwnerAddress = wifiP2pinfo.groupOwnerAddress;
                if (wifiP2pinfo.groupFormed && wifiP2pinfo.isGroupOwner) {
                    connectionStatus.setText("Host");
                    serverClass = new ServerClass();
                    serverClass.start();
                    setMessage(username);
                    openVersusDialog();
                    new credentialTask().execute();

                } else if(wifiP2pinfo.groupFormed) {
                    connectionStatus.setText("client");
                    clientClass = new ClientClass(groupOwnerAddress);
                    clientClass.start();
                    setMessage(username);
                    openVersusDialog();
                    new credentialTask().execute();


                }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        getNameDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getNameDialog.dismiss();
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;
        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                Log.v("MainActivity", "" + e);
            }
        }
    }
    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.v("MainActivity", "" + e);
            }
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.v("MainActivity", "" + e);
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.v("MainActivity", "" + e);
            }
        }
    }
    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }
        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                Log.v("MainActivity", "" + e);
            }
        }
    }
    public class credentialTask extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... strings) {
            isCredential = true;
            sendReceive.write((getMessage()).getBytes());
            isCredential = false;
            return null;

        }
    }
    public class MultiTask extends AsyncTask<String, String, String> {
        String msg;
        @Override
        protected String doInBackground(String... strings) {
            msg = writeMsg.getText().toString();
            sendReceive.write(msg.getBytes());
            Boolean isSent = false;

            while (isSent == false) {
                if (!(read_msg_box.getText().toString()).equals("Message")) {
                    isSent = true;
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (msg.equals(tempMsg)) {
                Toast.makeText(getApplicationContext(), "Zoinks!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "wagwon", Toast.LENGTH_SHORT).show();

            }


        }
    }


}
