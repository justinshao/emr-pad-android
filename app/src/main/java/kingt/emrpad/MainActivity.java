package kingt.emrpad;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wang.avi.AVLoadingIndicatorView;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends Activity implements AdvancedWebView.Listener, FingerprintCaptureListener {

    private static final String TAG = "emrpad";
    private static Pattern rIp = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d{1,5})*$");
    private static final String APP_ROOT_KEY = "appRoot";
    private static String AUDIO_TMP_PATH = null;
    private FingerSigner fingerSigner;
    private Camera picCapturer = new Camera(this);

    static {
        try {
            AUDIO_TMP_PATH = File.createTempFile("audio_tmp", ".3gp").getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AdvancedWebView mWebView;
    private AVLoadingIndicatorView loadingAvi;
    private AVLoadingIndicatorView audioAvi;
    private View fp;
    private ImageView fpimg;
    private ImageButton fpcancel;
    private ImageButton fpok;
    private long chdId = 0;
    private Audio mAudio = new Audio();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.verifyStoragePermissions(this);

        setContentView(R.layout.activity_main);

        fingerSigner = FingerSigner.getInstance(this);
        fingerSigner.setListener(this);
        loadingAvi = (AVLoadingIndicatorView) findViewById(R.id.loadingAvi);
        audioAvi = (AVLoadingIndicatorView) findViewById(R.id.audioAvi);
        fp = findViewById(R.id.fp);
        fpimg = (ImageView) findViewById(R.id.fpimg);
        fpcancel = (ImageButton) findViewById(R.id.fpcancel);
        fpok = (ImageButton) findViewById(R.id.fpok);
        mWebView = (AdvancedWebView) findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setCookiesEnabled(true);
        mWebView.clearCache(true);
        mWebView.addJavascriptInterface(this, "native");

        if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(getIntent().getAction())){
            startFingerSigner();
        }else {
            //TODO: 检测设备是否连接并打开
        }

        loadApp();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        // ...
    }
    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        // ...
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        fingerSigner.destroy();
        // ...
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case Camera.REQUEST_FOR_CAPTURE_PIC:
            case Camera.REQUEST_FOR_CAPTURE_VEDIO:
                if(resultCode == RESULT_OK){
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("是否保存？" )
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        if(requestCode ==  Camera.REQUEST_FOR_CAPTURE_VEDIO){
                                            uploadVedio();
                                        }else {
                                            uploadPic();
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(MainActivity.this, "上传失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).setNegativeButton("取消", null).show();
                }
                break;
            default:
                break;
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public void onBackPressed() {
        if (!mWebView.onBackPressed()) { return; }
        // ...
        super.onBackPressed();
    }
    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        showLoading();
    }
    @Override
    public void onPageFinished(String url) {
        hideLoading();
    }
    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
//        if(failingUrl.equals("http://" + getAppRoot() + "/") ||
//                failingUrl.equals("http://" + getAppRoot())){
//            showInputAppRootDialpg();
//        }
        hideLoading();
        showInputAppRootDialpg(getAppRoot());
    }
    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }
    @Override
    public void onExternalPageRequest(String url) { }

    public void fpOk(View view){
        if(chdId != 0){
            try {
                Bitmap bm =((BitmapDrawable)fpimg.getDrawable()).getBitmap();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                //String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                postSign(chdId, new ByteArrayInputStream(os.toByteArray()));
            }catch (Throwable e){
                Toast.makeText(this, "签名出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this, "未选择病历", Toast.LENGTH_SHORT).show();
        }
    }
    public void fpCancel(View view) {
        hideFp();
    }

    private void loadApp(){
        String appRoot = getAppRoot();

        if(TextUtils.isEmpty(appRoot)){
            showInputAppRootDialpg(appRoot);
        }else {
            String url = "http://" + appRoot;
            Log.d(TAG, "load: " + url);

            mWebView.loadUrl(url);
        }
    }
    private void showInputAppRootDialpg(String appRoot){
        final EditText inputHost = new EditText(this);
        inputHost.setText(appRoot == null ? "" : appRoot);
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputHost)
                .setCancelable(false)
                .setMessage("请输入App IP地址")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String appRoot = inputHost.getText().toString().trim();

                        if(!rIp.matcher(appRoot).matches()){
                            Toast.makeText(MainActivity.this, "IP地址格式错误", Toast.LENGTH_SHORT).show();

                            showInputAppRootDialpg(appRoot);
                        }else {
                            setAppRoot(appRoot);
                            loadApp();
                        }
                    }
                })
                //.setNegativeButton("取消", null)
                .show();
    }
    private String getAppRoot() {
        return getPreferences(MODE_PRIVATE).getString(APP_ROOT_KEY, null);
    }
    private void setAppRoot(String appRoot){
        getPreferences(MODE_PRIVATE).edit().putString(APP_ROOT_KEY, appRoot).commit();
    }
    private void showLoading(){
        loadingAvi.show();
        loadingAvi.bringToFront();
    }
    private void hideLoading(){
        loadingAvi.hide();
    }
    private void showAudioLoading(){
        audioAvi.bringToFront();
        audioAvi.show();
    }
    private void hideAudioLoading(){
        audioAvi.hide();
    }
    private void uploadAudio(long chdId) throws FileNotFoundException {
        this.uploadMedia(chdId, "audio", new File(AUDIO_TMP_PATH), "录音文件-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".3gp");
    }
    private void uploadPic() throws FileNotFoundException {
        this.uploadMedia(chdId, "pic", new File(picCapturer.getCapturePicUri().getPath()), "照片-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".jpg");
    }
    private void uploadVedio() throws FileNotFoundException {
        this.uploadMedia(chdId, "vedio", new File(picCapturer.getCaptureVedioUri().getPath()), "视频-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".mp4");
    }

    private void uploadMedia(long chdId, String name, File file, String fileName) throws FileNotFoundException {
        final String url = "http://" + getAppRoot() + "/api/UploadMedia";
        RequestParams params = new RequestParams();
        params.put("chdId", chdId);
        params.put("sourceType", 2);
        params.put("fileName", fileName);
        params.put(name, file, fileName);

        new AsyncHttpClient().post(url, params, new BaseJsonHttpResponseHandler<JsonResult>() {

            @Override
            public void onStart() {
                super.onStart();
                showLoading();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  Throwable e, String rawJsonData, JsonResult errorResponse) {
                hideLoading();
                Toast.makeText(MainActivity.this, "上传失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JsonResult response) {
                hideLoading();
                if(response.isOk() == false){
                    Toast.makeText(MainActivity.this, "上传失败：" + response.getMessage(), Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(MainActivity.this, "上传成功！", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected JsonResult parseResponse(String rawJsonData, boolean isFailure)
                    throws Throwable {
                try {
                    return new Gson().fromJson(rawJsonData, JsonResult.class);
                }catch (Throwable e){
                    return new JsonResult(false, "解析结果失败：" + e.getMessage());
                }
            }
        });
    }
    private void startFingerSigner(){
        try {
            fingerSigner.tryStart();

            Toast.makeText(this, "已检测并打开指纹设备", Toast.LENGTH_SHORT).show();
        }catch (Throwable e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void showFp(Bitmap bitmap){
        fpimg.setImageBitmap(bitmap);
        fp.setVisibility(View.VISIBLE);
    }
    private void hideFp(){
        fpimg.setImageBitmap(null);
        fp.setVisibility(View.GONE);
    }
    private void postSign(final long chdId, InputStream pic){
        final String url = "http://" + getAppRoot() + "/api/PatSign?d=1";
        RequestParams params = new RequestParams();
        params.put("chdId", chdId);
        params.put("signpic", pic, "signpic.png");

        new AsyncHttpClient().post(url, params, new BaseJsonHttpResponseHandler<JsonResult>() {

            private void notityResult(boolean ok, long chdId){
                String fn = "javascript:mobileApi.notifyPatSignResult(" + String.valueOf(ok) + "," + String.valueOf(chdId) + ")";
                mWebView.loadUrl(fn);
            }

            @Override
            public void onStart() {
                super.onStart();
                showLoading();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  Throwable e, String rawJsonData, JsonResult errorResponse) {
                hideLoading();

                Toast.makeText(MainActivity.this, "签名出错：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JsonResult response) {
                hideLoading();
                if(response.isOk() == false){
                    Toast.makeText(MainActivity.this, "签名出错：" + response.getMessage(), Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(MainActivity.this, "签名成功！", Toast.LENGTH_LONG).show();

                    hideFp();
                }

                this.notityResult(response.isOk(), chdId);
            }

            @Override
            protected JsonResult parseResponse(String rawJsonData, boolean isFailure)
                    throws Throwable {
                try {
                    return new Gson().fromJson(rawJsonData, JsonResult.class);
                }catch (Throwable e){
                    return new JsonResult(false, "解析结果失败：" + e.getMessage());
                }
            }
        });
    }

    @android.webkit.JavascriptInterface
    public void saveAppRoot(final String appRoot ){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!TextUtils.isEmpty(appRoot)){
                    if(!rIp.matcher(appRoot).matches()){
                        Toast.makeText(MainActivity.this, "IP地址格式错误", Toast.LENGTH_SHORT).show();
                        showInputAppRootDialpg(appRoot);
                    }else {
                        setAppRoot(appRoot);
                        loadApp();
                    }
                }
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void record(final long chdId){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!mAudio.isStarting()){
                    mAudio.setOutputFile(AUDIO_TMP_PATH);
                    try {
                        mAudio.start();
                        showAudioLoading();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "出错了 >_<：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void stopRecord(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(mAudio.isStarting()){
                    mAudio.stop();
                    hideAudioLoading();
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("是否保存录音？" )
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        uploadAudio(chdId);
                                    } catch (Exception e) {
                                        Toast.makeText(MainActivity.this, "上传失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void takePhoto(final long chdId){
        if(this.chdId > 0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    picCapturer.takePhoto();
                }
            });
        }
    }
    @android.webkit.JavascriptInterface
    public void takeVideo(final long chdId){
        if(this.chdId > 0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    picCapturer.takeVedio();
                }
            });
        }
    }
    @android.webkit.JavascriptInterface
    public void selectEmr(final long chdId){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.chdId = chdId;
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void clearEmr(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.chdId = 0;
                hideFp();
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void openFingerSigner(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FingerSigner.getInstance(MainActivity.this).tryStart();

                    Toast.makeText(MainActivity.this, "已打开指纹设备", Toast.LENGTH_SHORT).show();
                }catch (Throwable t){
                    Toast.makeText(MainActivity.this, "打开失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void error(final String error){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void toast(final String info){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void viewPic(final long id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playMedia(id, "image/*");
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void playAudio(final long id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playMedia(id, "audio/*");
            }
        });
    }
    @android.webkit.JavascriptInterface
    public void playVedio(final long id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playMedia(id, "video/*");
            }
        });
    }
    public void playMedia(final long id, final String type){
        final File mediaFile = new File(Utils.TMP_MEDIA_FILE_DIR.getPath() + File.separator + "media_" + id);
        final String url = "http://" + getAppRoot() + "/api/MediaFile/" + id;

        if(mediaFile.exists()){
            startMediaActivity(mediaFile, type);
        }else{
            showLoading();
            new AsyncHttpClient().get(url, new BinaryHttpResponseHandler(new String[]{
                    RequestParams.APPLICATION_OCTET_STREAM, "video/.*", "audio/.*", "image/.*" }){
                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                    hideLoading();

                    try (OutputStream os = new FileOutputStream(mediaFile, false);){
                        os.write(binaryData, 0, binaryData.length);
                        os.flush();

                        startMediaActivity(mediaFile, type);
                    }catch (IOException e){
                        Toast.makeText(MainActivity.this, "保存文件出错", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                    hideLoading();

                    Toast.makeText(MainActivity.this, "下载文件出错", Toast.LENGTH_SHORT).show();

                    Log.e(TAG, error.getMessage());
                    error.printStackTrace();
                }
            });
        }
    }
    private void startMediaActivity(File mediaFile, String type){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(mediaFile), type);
        startActivity(intent);
    }

    @Override
    public void captureOK(final byte[] bytes) {
        final int width = fingerSigner.getImageWidth();
        final int height = fingerSigner.getImageHeight();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(chdId == 0){
                    Toast.makeText(MainActivity.this, "请选择病历", Toast.LENGTH_SHORT).show();
                }else if (bytes != null) {
                    Log.d(TAG, "captureOK: bytes=" + String.valueOf(bytes.length));

                    showFp(ToolUtils.renderCroppedGreyScaleBitmap(bytes, width, height));
                    fp.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    @Override
    public void captureError(final FingerprintException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "captureError: " + e.getMessage());
            }
        });
    }
    @Override
    public void extractOK(final byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "extractOK: bytes=" + String.valueOf(bytes.length));
            }
        });
    }
    @Override
    public void extractError(final int i) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "extractError: " + String.valueOf(i));
            }
        });
    }
}
