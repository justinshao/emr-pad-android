package kingt.emrpad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

/**
 * Created by shao on 2017/5/11.
 */

public class FingerSignerUsbDetact extends BroadcastReceiver
{
    private static final String TAG = "FingerSignerUsbDetact";
    @Override
    public synchronized void onReceive(Context context, Intent intent) {

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            try {
                FingerSigner.getInstance(context).tryStart();

                Toast.makeText(context, "已检测到并打开指纹设备", Toast.LENGTH_SHORT).show();
            }catch (Throwable e){
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equalsIgnoreCase(intent.getAction())){
            FingerSigner.getInstance(context).destroy();

            Toast.makeText(context, "指纹设备已断开", Toast.LENGTH_SHORT).show();
        }
    }
}