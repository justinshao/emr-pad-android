package kingt.emrpad;

import android.content.Context;

import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by shao on 2017/5/11.
 */

public class FingerSigner {
    private boolean start = false;
    private FingerprintCaptureListener listener;
    private transient FingerprintSensor fingerprintSensor;
    private Context context;

    private static transient FingerSigner instance;

    private FingerSigner(Context context){
        this.context = context;
    }

    private synchronized FingerprintSensor ensureFingerprintSensor(){
        if(fingerprintSensor == null){
            Map fpParams = new HashMap();
            fpParams.put(ParameterHelper.PARAM_KEY_VID, 6997);
            fpParams.put(ParameterHelper.PARAM_KEY_PID, 288);
            fingerprintSensor = FingprintFactory.createFingerprintSensor(context, TransportType.USB, fpParams);
        }

        return fingerprintSensor;
    }

    public static synchronized FingerSigner getInstance(Context context){
        if(instance == null){
            instance = new FingerSigner(context);
        }

        return instance;
    }

    public void setListener(FingerprintCaptureListener listener){
        this.listener = listener;
    }
    public synchronized void tryStart(){
        this.destroy();
        this.open();
    }
    private synchronized void open(){
        ensureFingerprintSensor();
        try {
            fingerprintSensor.open(0);
            fingerprintSensor.setFingerprintCaptureListener(0, listener);
            fingerprintSensor.startCapture(0);
        } catch (FingerprintException e) {
            throw new RuntimeException(getErrorMessage(e.getErrorCode()), e);
        }
    }
    private synchronized void stop(){
        try {
            fingerprintSensor.stopCapture(0);
            fingerprintSensor.close(0);
        } catch (FingerprintException e) {
            throw new RuntimeException(getErrorMessage(e.getErrorCode()), e);
        }
    }
    public synchronized void destroy(){
        if(fingerprintSensor != null){
            this.stop();
            fingerprintSensor.destroy();
            fingerprintSensor = null;
        }
    }

    public int getImageWidth(){
        if(fingerprintSensor == null){
            throw new RuntimeException("设备未打开");
        }

        return fingerprintSensor.getImageWidth();
    }
    public int getImageHeight(){
        if(fingerprintSensor == null){
            throw new RuntimeException("设备未打开");
        }

        return fingerprintSensor.getImageHeight();
    }

    public String getErrorMessage(int errorCode){
        String msg = null;

        switch (errorCode){
            case -20001:
                msg =  "打开设备失败";
                break;
            case -20002:
                msg =  "关闭设备失败";
                break;
            case -20003:
                msg =  "获取GPIO失败";
                break;
            case -20004:
                msg =  "设置GPIO失败";
                break;
            case -20005:
                msg =  "读EEPROM失败";
                break;
            case -20006:
                msg =  "从USB获取图像失败";
                break;
            case -20007:
                msg =  "探测USB图像失败";
                break;
            case -20008:
                msg =  "输入缓存不够";
                break;
            case -20009:
                msg =  "读取数据异常";
                break;
            case -20010:
                msg =  "采集指纹失败";
                break;
            case -20011:
                msg =  "解密图像数据失败";
                break;
            case -20012:
                msg =  "启动采集线程失败";
                break;
            case -20013:
                msg =  "停止采集线程失败";
                break;
            case -20014:
                msg =  "初始化指纹设备失败";
                break;
            case -20015:
                msg =  "设置矫正参数失败";
                break;
            case -5000:
                msg =  "没有找到指定id指纹模板";
                break;
            case -5002:
                msg =  "参数错误";
                break;
            case -5003:
                msg =  "指纹模板错误";
                break;
            case -5004:
                msg =  "方法错误";
                break;
            default:
                msg =  "未知错误";
        }

        return msg + "(" + String.valueOf(errorCode) + ")";
    }
}
