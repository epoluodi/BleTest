package fillsquad.com.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import cc.liyongzhi.ecgview.ECGView;

public class MainActivity extends AppCompatActivity {

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    BluetoothGattService bluetoothGattService;
    BluetoothGattCharacteristic bluetoothGattCharacteristic;
    BluetoothGattDescriptor bluetoothGattDescriptor1, bluetoothGattDescriptor2;

    Timer timer;
    Button btn1, btn2, btn3, btn4, btn5;
    EditText txt;

    EditText editText;
    Button btnset;

    byte[] bytes1;//轮训
    byte[] bytesch;//通道

    byte bhead =(byte) 0xBB;

    ECGView view;
    short[] shorts = new short[1];
    private LinkedBlockingQueue queue = new LinkedBlockingQueue();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            shorts[0]=0;
            queue.put(shorts);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        new DataGeneratingThread(queue).start();
        bytes1 = new byte[5];
        bytes1[0] = bhead;
        bytes1[1] = (byte) 5;
        bytes1[2] = (byte) 12;
        bytes1[3] = (byte) 1;
        bytes1[4] = (byte) 76;

        view = (ECGView)findViewById(R.id.ecg);
        view.setSubViewNum(12);
        view.setColumnSubViewNum(2);
        ArrayList<String> text = new ArrayList<>();
        String[] s = new String[]{"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
        Collections.addAll(text, s);
        view.setText(text);
        view.setInputChannelNum(1);

        view.setChannel(queue);

        editText = (EditText)findViewById(R.id.edittxt);
        btnset=(Button)findViewById(R.id.btnset);
        btn1 = (Button) findViewById(R.id.btn1);
        btn2 = (Button) findViewById(R.id.btn2);
        btn3 = (Button) findViewById(R.id.btn3);
        btn4 = (Button) findViewById(R.id.btn4);
        btn5 = (Button) findViewById(R.id.btn5);
        txt = (EditText) findViewById(R.id.txt);
        txt.setFocusableInTouchMode(false);

        btnset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editText.getText().toString().equals(""))
                    return;
                int ichannel;
                try
                {
                    ichannel = Integer.parseInt(editText.getText().toString());
                }catch (Exception e)
                {
                    e.printStackTrace();
                    return;
                }
                bytesch = new byte[5];
                bytesch[0] = bhead;
                bytesch[1] = (byte) 5;
                bytesch[2] = (byte) 1;
                switch (ichannel)
                {
                    case 1:
                        bytesch[3] = (byte) 0x21;
                        break;
                    case 2:
                        bytesch[3] = (byte) 0x22;
                        break;
                    case 3:
                        bytesch[3] = (byte) 0x23;
                        break;
                    case 4:
                        bytesch[3] = (byte) 0x24;
                        break;
                    case 5:
                        bytesch[3] = (byte) 0x25;
                        break;
                }

                byte sum=0;
                for (int i =0 ;i<bytesch.length-1;i++)
                {
                    sum ^=bytesch[i];
                }
                bytesch[4] = (byte) ~sum;
                StringBuilder stringBuilder = new StringBuilder();
                for (byte byteChar : bytesch)
                    stringBuilder.append(String.format("%02X ", byteChar));
                txt.setText("发送数据数据:" + stringBuilder.toString() + "\n");
                bluetoothGattCharacteristic.setValue(bytesch);
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothGattCharacteristic != null) {

                    byte[] bytes = new byte[5];
                    bytes[0] = (byte) 187;
                    bytes[1] = (byte) 5;
                    bytes[2] = (byte) 11;
                    bytes[3] = (byte) 0;
                    bytes[4] = (byte) 74;
                    bluetoothGattCharacteristic.setValue(bytes);
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                }

                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                StringBuilder stringBuilder = new StringBuilder();

                for (byte byteChar : bytes1)
                    stringBuilder.append(String.format("%02X ", byteChar));

//                Message message = handler.obtainMessage();
//                message.obj = "发送数据数据:" + stringBuilder.toString() + "\n";
//                handler.sendMessage(message);

                txt.setText("发送数据数据:" + stringBuilder.toString() + "\n");
                timer=new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (bluetoothGattCharacteristic != null) {
                            bluetoothGattCharacteristic.setValue(bytes1);
                            bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                        }
                    }
                };

                timer.schedule(timerTask, 0, 60);

                btn3.setEnabled(false);
                btn1.setEnabled(false);


            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                timer.cancel();
                timer.purge();
                timer=null;
                btn3.setEnabled(true);
                btn1.setEnabled(true);
            }
        });
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txt.setText("");
            }
        });
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }





    }

    @Override
    protected void onResume() {
        super.onResume();

        view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.stop();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    txt.setText(msg.obj.toString() + "\n" + txt.getText().toString());
                    break;


            }
        }
    };


    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.i("搜索 设备名称", device.getName());//84:DD:20:ED:1F:48
            Log.i("搜索 信号", device.getAddress());
            Log.i("搜索 信号", String.valueOf(rssi));


            if (device.getName().equals("SPSD")) {

                Message message = handler.obtainMessage();
                message.obj = "设备名称:" + device.getName() + "\n" +
                        "设备地址:" + device.getAddress() + "\n";

                handler.sendMessage(message);

                bluetoothGatt = device.connectGatt(MainActivity.this, false, bluetoothGattCallback);
                bluetoothGatt.connect();
                bluetoothAdapter.stopLeScan(leScanCallback);
            }

        }
    };


    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);


            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("连接服务，开始扫描蓝牙中得服务", "----》");
//                gatt.connect();
                bluetoothGatt.discoverServices();
//                try {
//                    Thread.sleep(500);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }


            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

//            List<BluetoothGattService> bluetoothGattServices = bluetoothGatt.getServices();
//            Log.i("bluetoothGattServices",String.valueOf(bluetoothGattServices.size()));

            bluetoothGattService = bluetoothGatt.getService(UUID.fromString(
                    "0000ffe0-0000-1000-8000-00805f9b34fb"));
            if (bluetoothGattService == null) {
                Log.i("bluetoothGattService", "null");
                return;
            }
            Message message = handler.obtainMessage();
            message.obj = "发现服务:\n" + bluetoothGattService.getUuid().toString() + "\n";
            handler.sendMessage(message);

//            for (BluetoothGattCharacteristic bluetoothGattCharacteristics : bluetoothGattService.getCharacteristics())
////            {
////                message = handler.obtainMessage();
////                message.obj = "发现desc:\n" + bluetoothGattDescriptor.getUuid().toString() + "\n";
////                handler.sendMessage(message);
//                Log.i("desc:",bluetoothGattCharacteristics.getUuid().toString());
//////                00002902-0000-1000-8000-00805f9b34fb
//////                00002901-0000-1000-8000-00805f9b34fb
////            }


            bluetoothGattCharacteristic =
                    bluetoothGattService.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));

            if (bluetoothGattCharacteristic == null) {

                return;
            }
            message = handler.obtainMessage();
            message.obj = "发现Chara:\n" + bluetoothGattCharacteristic.getUuid().toString() + "\n";
            handler.sendMessage(message);
            bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            bluetoothGatt.readRemoteRssi();
//            for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattCharacteristic.getDescriptors())
//            {
//                message = handler.obtainMessage();
//                message.obj = "发现desc:\n" + bluetoothGattDescriptor.getUuid().toString() + "\n";
//                handler.sendMessage(message);
//                Log.i("desc:",bluetoothGattDescriptor.getUuid().toString());
////                00002902-0000-1000-8000-00805f9b34fb
////                00002901-0000-1000-8000-00805f9b34fb
//            }
            bluetoothGattDescriptor1 = bluetoothGattCharacteristic.getDescriptors().get(0);
            bluetoothGattDescriptor2 = bluetoothGattCharacteristic.getDescriptors().get(1);

//            bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
//        bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"));
//        bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
//        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);


        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            byte[] value = characteristic.getValue();

            StringBuilder stringBuilder = new StringBuilder("");
            if (value == null || value.length <= 0) {
                return;
            }
            for (byte byteChar : value)
                stringBuilder.append(String.format("%02X ", byteChar));

//            Log.i("接收到得数据",stringBuilder.toString());


            Message message = handler.obtainMessage();
            message.obj = "数据:" + stringBuilder.toString() + "\n";
            handler.sendMessage(message);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i("onCharacteristicChanged", characteristic.getUuid().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();

            StringBuilder stringBuilder = new StringBuilder("");
            if (value == null || value.length <= 0) {
                return;
            }
            for (byte byteChar : value)
                stringBuilder.append(String.format("%02X ", byteChar));

//            Log.i("接收到得数据",stringBuilder.toString());

            int ivalue= bytesToInt2(value,1);
            double dvalue = ivalue *2*1.8 /1.4 / (Math.pow(2,24)-1);
            short[] shorts1 = new short[1];
            shorts1[0] = (short) dvalue;
            try {
                queue.put(shorts1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DecimalFormat df = new java.text.DecimalFormat("#.00000");
            String strvalue=df.format(dvalue);


            Message message = handler.obtainMessage();
            message.obj = "数据:" + stringBuilder.toString() + "\n " + "数值:"+strvalue+"\n";
            handler.sendMessage(message);
//            if (characteristic.getUuid().toString().equals("0000fff4-0000-1000-8000-00805f9b34fb")) {
//
//                BluetoothGattDescriptor localBluetoothGattDescriptor =
//                        characteristic.getDescriptor(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"));
//                localBluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                bluetoothGatt.writeDescriptor(localBluetoothGattDescriptor);
//            } else {
//                bluetoothGatt.setCharacteristicNotification(characteristic, true);
//            }


        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            byte[] value = descriptor.getValue();

            StringBuilder stringBuilder = new StringBuilder("");
            if (value == null || value.length <= 0) {
                return;
            }
            for (byte byteChar : value)
                stringBuilder.append(String.format("%02X ", byteChar));


//            for (int i = 0; i < value.length; i++) {
//                int v = value[i] & 0xFF;
//                String hv = Integer.toHexString(v);
//                if (hv.length() < 2) {
//                    stringBuilder.append(0);
//                }
//                stringBuilder.append(hv);
//            }
//            txt.setText(stringBuilder);

            Message message = handler.obtainMessage();
            message.obj = "desc 数据:" + stringBuilder.toString() + "\n";
            handler.sendMessage(message);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.i("onReadRemoteRssi 信号", String.valueOf(rssi));
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src
     *            byte数组
     * @param offset
     *            从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset+1] & 0xFF)<<8)
                | ((src[offset+2] & 0xFF)<<16)
                | ((src[offset+3] & 0xFF)<<24));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) ( ((src[offset] & 0xFF)<<24)
                |((src[offset+1] & 0xFF)<<16)
                |((src[offset+2] & 0xFF)<<8)
                |(src[offset+3] & 0xFF));
        return value;
    }


    void read() {


//
        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        bluetoothAdapter.startLeScan(leScanCallback);


    }
}
