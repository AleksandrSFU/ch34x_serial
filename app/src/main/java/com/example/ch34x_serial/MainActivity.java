package com.example.ch34x_serial;
import androidx.appcompat.app.AppCompatActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.widget.Toast;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PREMISSION = "com.hariharan.arduinousb.USB_PREMISSION";
    UsbDevice usbDevice;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection usbConnection;
    Button btnInitPort;
    Button btnClosePort;  TextView tvViewport;
    private int limit = 1023;
    ByteBuffer buffer = ByteBuffer.allocate(limit);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        btnInitPort = findViewById(R.id.InitPort);
        btnClosePort = findViewById(R.id.ClosePort);
        tvViewport = findViewById(R.id.Viewport);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PREMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(broadcastReceiver, filter);

    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PREMISSION)){
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted){
                    usbConnection = usbManager.openDevice(usbDevice);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(usbDevice,usbConnection);
                    if (serialPort != null){
                        if (serialPort.open()) {
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(callback);
                            Toast.makeText(context, "ПОРТ ОТКРЫТ !", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context,"ПОРТ ЗАКРЫТ",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context,"ПОРТ ПУСТОЙ",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context,"БЛОКИРОВКА",Toast.LENGTH_SHORT).show();
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                onClickConnect(btnInitPort);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
                onClickDisconnect(btnClosePort);
            }
        }
    };

    UsbSerialInterface.UsbReadCallback callback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] bytes) {

            // String data = new String(bytes, StandardCharsets.UTF_8); // вывод строки из uart
            // tvViewport.setText(data);                                // вывод строки на экран

            StringBuffer str = new StringBuffer(limit);
            str.delete(0,limit);

            for (byte i : bytes) {
                int decimal = (int)i & 0XFF;
                if (decimal != 0XC0) {                           // начал/конец строки
                    str.append(Integer.toHexString(decimal));
                    if (decimal == 0XFF) {str.delete(0, limit);} // остаток 193 до 255 байты убраны.
                } else {
                        tvViewport.setText(str.toString());
                        str.delete(0, limit);
                }
            }
        }
    };

    public void onClickConnect (View View) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if(!usbDevices.isEmpty()){
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry:usbDevices.entrySet()){
                usbDevice = entry.getValue();

                // из строки usbDevice.toString() вытащить deviceID
                // int deviceID = usbDevice.getDeviceId();
                // Viewport.setText(usbDevice.toString());
                PendingIntent pi = PendingIntent.getBroadcast(this, 0,  new Intent(ACTION_USB_PREMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
                usbManager.requestPermission(usbDevice,pi);
                keep = false;
                // ------------ Type-С номер соединяемого устройства ---------
                //if (deviceID == 6790 || deviceID == 1659){
                //    PendingIntent pi = PendingIntent.getBroadcast(this, 0,  new Intent(ACTION_USB_PREMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
                //    usbManager.requestPermission(usbDevice,pi);
                //    keep = false;
                //} else {
                //    usbConnection = null;
                //    usbDevice = null;
                //}
                //------------------------------------------------------------
                if (!keep){
                    break;
                }
            }
        }
    }
    public void onClickDisconnect (View View) {
         serialPort.close();
    }
}
