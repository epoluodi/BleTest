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
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
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

    Timer timer, timer2;
    Button btn1, btn2, btn3, btn4, btn5;
    EditText txt;

    EditText editText;
    Button btnset;
    LinearLayout linearLayout;
    Queue<Double> queue;

    byte[] bytes1;//轮训
    byte[] bytesch;//通道

    boolean isstart=true;
    byte[] allbuffer;
    int buffercount;

    byte bhead = (byte) 0xBB;
    FileWriter writer;
    //    ECGView view;
    short[] shorts = new short[1];

    //    private LinkedBlockingQueue queue = new LinkedBlockingQueue();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = new LinkedBlockingQueue<>();
        allbuffer = new byte[60];
        buffercount= 1;
//        try {
//            shorts[0]=0;
////            queue.put(shorts);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        new DataGeneratingThread(queue).start();
        bytes1 = new byte[5];
        bytes1[0] = bhead;
        bytes1[1] = (byte) 5;
        bytes1[2] = (byte) 12;
        bytes1[3] = (byte) 1;
        bytes1[4] = (byte) 76;
        File file=new File(Environment.getExternalStorageDirectory(),"data.csv");
        try {
            writer = new FileWriter(file, true);
        }
        catch (Exception e)
        {e.printStackTrace();}


//
//        view = (ECGView)findViewById(R.id.ecg);
//        view.setSubViewNum(12);
//        view.setColumnSubViewNum(2);
//        ArrayList<String> text = new ArrayList<>();
//        String[] s = new String[]{"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
//        Collections.addAll(text, s);
//        view.setText(text);
//        view.setInputChannelNum(1);
////        view.setScaleDetail(1);
//
//        view.setChannel(queue);


        editText = (EditText) findViewById(R.id.edittxt);
        btnset = (Button) findViewById(R.id.btnset);
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
                try {
                    ichannel = Integer.parseInt(editText.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                bytesch = new byte[6];
                bytesch[0] = bhead;
                bytesch[1] = (byte) 6;
                bytesch[2] = (byte) 3;
                switch (ichannel) {
                    case 1:
                        bytesch[3] = (byte) 0x01;
                        break;
                    case 2:
                        bytesch[3] = (byte) 0x02;
                        break;
                    case 3:
                        bytesch[3] = (byte) 0x03;
                        break;
                    case 4:
                        bytesch[3] = (byte) 0x04;
                        break;
                    case 5:
                        bytesch[3] = (byte) 0x05;
                        break;
                }
                bytesch[4] = 0;
                byte sum = 0;
                for (int i = 0; i < bytesch.length - 1; i++) {
                    sum ^= bytesch[i];
                }
                bytesch[5] = (byte) ~sum;
                StringBuilder stringBuilder = new StringBuilder();
                for (byte byteChar : bytesch)
                    stringBuilder.append(String.format("%02X ", byteChar));
                txt.setText("发送数据数据:" + stringBuilder.toString() + "\n");
                Log.i("设置通道", stringBuilder.toString());
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

                try {
                    writer.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                StringBuilder stringBuilder = new StringBuilder();
//
//                for (byte byteChar : bytes1)
//                    stringBuilder.append(String.format("%02X ", byteChar));

//                Message message = handler.obtainMessage();
//                message.obj = "发送数据数据:" + stringBuilder.toString() + "\n";
//                handler.sendMessage(message);

//                txt.setText("发送数据数据:" + stringBuilder.toString() + "\n");
//                timer = new Timer();
//                TimerTask timerTask = new TimerTask() {
//                    @Override
//                    public void run() {
//                        if (isstart) {
//                            if (bluetoothGattCharacteristic != null) {
//                                bluetoothGattCharacteristic.setValue(bytes1);
//                                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
//                            }
//                        }
//                    }
//                };
////
//                timer.schedule(timerTask, 0, 25);
////
                btn3.setEnabled(false);
                btn1.setEnabled(false);
//
//
                timer2 = new Timer();
                TimerTask timerTask1 = new TimerTask() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0);
                    }
                };
                timer2.schedule(timerTask1, 10, 10);


            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {




//                timer.cancel();
//                timer.purge();
//                timer = null;
                timer2.cancel();
                timer2.purge();
                timer2 = null;
                btn3.setEnabled(true);
                btn1.setEnabled(true);
            }
        });
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDataset.clear();
                txt.setText("");

                if (bluetoothGattCharacteristic != null) {

                    byte[] bytes = new byte[5];
                    bytes[0] = (byte) 187;
                    bytes[1] = (byte) 5;
                    bytes[2] = (byte) 11;
                    bytes[3] = (byte) 1;
                    bytes[4] = (byte) 75;
                    bluetoothGattCharacteristic.setValue(bytes);
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                }


//                byte[] bytes1 = new byte[5];
//                bytes1[0] = bhead;
//                bytes1[1] = (byte) 5;
//                bytes1[2] = (byte) 0x0B;
//                bytes1[3] = (byte) 1;
//                bytes1[4] = (byte) 0x4B;
//
//                bluetoothGattCharacteristic.setValue(bytes1);
//                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);

            }
        });
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }


        //初始化图表
        initChart(null, null, 0, xMax, yMin, yMax);


    }


    //图表相关
    private XYSeries series;
    private XYMultipleSeriesDataset mDataset;
    private GraphicalView chart;
    private XYMultipleSeriesRenderer renderer;
    private Context context;
    private int yMax = 2;//y轴最大值，根据不同传感器变化
    private int xMax = 200;//一屏显示测量次数
    private int yMin = 0;

    private int addX = -1;
    private double addY = 0;
    public double AVERAGE = 0;//存储平均值

    private void updateChart() {

        //设置好下一个需要增加的节点

//        addY = AVERAGE;//需要增加的值
        try {
            addY = queue.remove();
        } catch (Exception e) {
            return;
        }
        //移除数据集中旧的点集
        mDataset.removeSeries(series);

        //判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
        int length = series.getItemCount();
        if (length > 5000) {//设置最多5000个数
            length = 5000;
        }

        //注释掉的文字为window资源管理器效果

        //将旧的点集中x和y的数值取出来放入backup中，并且将x的值加1，造成曲线向右平移的效果
//	     for (int i = 0; i < length; i++) {
//		     xv[i] = (int) series.getX(i) + 1;
//		     yv[i] = (int) series.getY(i);
//	     }

        //点集先清空，为了做成新的点集而准备
//	     series.clear();

        //将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
        //这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点
        //每一个新点坐标都后移一位
        series.add(addX++, addY);//最重要的一句话，以xy对的方式往里放值
//	     for (int k = 0; k < length; k++) {
//	         series.add(xv[k], yv[k]);//把之前的数据放进去
//	     }
        if (addX > xMax) {
            renderer.setXAxisMin(addX - xMax);
            renderer.setXAxisMax(addX);
        }
//        renderer.setYAxisMin(1.2538480003661452);
//        renderer.setYAxisMax(1.2565126959308406);

//        renderer.setYAxisMin(renderer.getYAxisMin());
//        renderer.setYAxisMax(renderer.getYAxisMax());
        //重要：在数据集中添加新的点集
        mDataset.addSeries(series);

        //视图更新，没有这一步，曲线不会呈现动态
        //如果在非UI主线程中，需要调用postInvalidate()，具体参考api
        chart.invalidate();
    }

    /**
     * 初始化图表
     */
    private void initChart(String xTitle, String yTitle, int minX, int maxX, int minY, int maxY) {
        //这里获得main界面上的布局，下面会把图表画在这个布局里面
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        layout.setBackgroundColor(Color.WHITE);
        //这个类用来放置曲线上的所有点，是一个点的集合，根据这些点画出曲线
        series = new XYSeries("心跳曲线");
        //创建一个数据集的实例，这个数据集将被用来创建图表
        mDataset = new XYMultipleSeriesDataset();
        //将点集添加到这个数据集中
        mDataset.addSeries(series);

        //以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
        int lineColor = Color.BLACK;
        PointStyle style = PointStyle.CIRCLE;
        renderer = buildRenderer(lineColor, style, true);

        //设置好图表的样式
        setChartSettings(renderer, xTitle, yTitle,
                minX, maxX, //x轴最小最大值
                minY, maxY, //y轴最小最大值
                Color.GREEN, //坐标轴颜色
                Color.GREEN//标签颜色
        );

        //生成图表
        chart = ChartFactory.getLineChartView(this, mDataset, renderer);
        chart.setBackground(getResources().getDrawable(R.mipmap.backjpg));
//        chart.setBackgroundColor(Color.WHITE);
        //将图表添加到布局中去
        layout.addView(chart, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }


    protected XYMultipleSeriesRenderer buildRenderer(int color, PointStyle style, boolean fill) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

        //设置图表中曲线本身的样式，包括颜色、点的大小以及线的粗细等
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(color);
        r.setPointStyle(style);
        r.setFillPoints(fill);
        r.setLineWidth(2);//这是线宽
        renderer.addSeriesRenderer(r);

//        XYSeriesRenderer r1 = new XYSeriesRenderer();
//        r1.setColor(color);
//        r1.setPointStyle(style);
//        r1.setFillPoints(fill);
//        r1.setLineWidth(1);//这是线宽
//        renderer.addSeriesRenderer(r1);

        return renderer;
    }

    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
                                    double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
        //有关对图表的渲染可参看api文档
//        renderer.setChartTitle("心跳");//设置标题
//        renderer.setChartTitleTextSize(20);
        renderer.setXAxisMin(xMin);//设置x轴的起始点
        renderer.setXAxisMax(xMax);//设置一屏有多少个点
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
//        renderer.setXLabelsPadding(50);
//        renderer.setYLabelsPadding(50);
        renderer.setShowLabels(false);
        renderer.setMargins(new int[]{0,0,0,0});


//        renderer.setBackgroundColor(Color.WHITE);
//        renderer.setLabelsColor(Color.BLACK);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
        renderer.setShowGrid(false);
        renderer.setMarginsColor(Color.WHITE);
        renderer.setGridColor(Color.RED);//设置格子的颜色
        renderer.setXLabels(15);//没有什么卵用
        renderer.setYLabels(20);//把y轴刻度平均分成多少个
//        renderer.setLabelsTextSize(60);
//        renderer.setXTitle(xTitle);//x轴的标题
//        renderer.setYTitle(yTitle);//y轴的标题
//        renderer.setAxisTitleTextSize(60);
        renderer.setYLabelsAlign(Paint.Align.RIGHT);
        renderer.setPointSize((float) 2);
        renderer.setShowLegend(false);//说明文字
        renderer.setLegendTextSize(20);

    }

    @Override
    protected void onResume() {
        super.onResume();

//        view.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        view.stop();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (msg.obj != null)
                        txt.setText(msg.obj.toString() + "\n");
                    updateChart();
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

//            HC-08
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
//            bluetoothGattDescriptor1 = bluetoothGattCharacteristic.getDescriptors().get(0);
//            bluetoothGattDescriptor2 = bluetoothGattCharacteristic.getDescriptors().get(1);

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

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] value = characteristic.getValue();


//            StringBuilder stringBuilder = new StringBuilder("");
//            if (value == null || value.length <= 0) {
//                return;
//            }
//            for (byte byteChar : value)
//                stringBuilder.append(String.format("%02X ", byteChar));
//
//
//            isstart=false;
//
//            Log.e("长度",String.valueOf(value.length)+" "+stringBuilder.toString());
//



            for (int i=0;i<5;i++)
            {
                byte[] buffer = new byte[4];
                buffer[0] = 0;
                buffer[1] = value[i*4+1];
                buffer[2] = value[i*4+2];
                buffer[3] = value[i*4+3];

                int ivalue = bytesToInt2(buffer, 0);
                double dvalue = ((double) ivalue) * 2 * 1.8 / 1.4 / (Math.pow(2, 24) - 1);
                AVERAGE = dvalue;
                Log.e("数值", String.valueOf(AVERAGE));
                queue.add(AVERAGE);
                try {
                    writer.write(String.valueOf(AVERAGE)+",\n");
                }
                catch (Exception e)
                {e.printStackTrace();}

            }
//            isstart=true;
//            System.arraycopy(value,0,allbuffer,(buffercount-1)*20,20);
//
//            if (buffercount ==6) {
//
//
//                for (int i = 0; i < 10; i++) {
//
//
//                    byte[] buffer = new byte[4];
//                    buffer[0] = 0;
//                    System.arraycopy(allbuffer, i * 6 + 2, buffer, 1, 3);
//
////                buffer[1] = value[2];
////                buffer[2] = value[3];
////                buffer[3] = value[4];
//                    int ivalue = bytesToInt2(buffer, 0);
//                    double dvalue = ((double) ivalue) * 2 * 1.8 / 1.4 / (Math.pow(2, 24) - 1);
////            dvalue = (dvalue*1000 -1330)*10;
////            dvalue*=10;
////                short[] shorts1 = new short[1];
////                shorts1[0] = (short) dvalue;
//                    AVERAGE = dvalue;
//                    Log.e("数值", String.valueOf(AVERAGE));
//                    queue.add(AVERAGE);
//                }
//                buffercount=1;
//            }
//            else
//            {
//                buffercount++;
//            }
//
////            try {
////                queue.put(shorts1);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//            DecimalFormat df = new java.text.DecimalFormat("#.000000");
//            String strvalue = df.format(dvalue);
//
//
//            Message message = handler.obtainMessage();
//            message.obj = "数据:" + stringBuilder.toString() + "\n " + "数值:" + AVERAGE + "\n";
//            handler.sendMessage(message);
//


        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);


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
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
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
