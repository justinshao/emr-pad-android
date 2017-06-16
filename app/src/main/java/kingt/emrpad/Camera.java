package kingt.emrpad;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

public class Camera {
    public static final int REQUEST_FOR_CAPTURE_PIC = 1;
    public static final int REQUEST_FOR_CAPTURE_VEDIO = 2;

    private Activity mActivity;
    private Uri mCapturePicUri;
    private Uri mCaptureVedioUri;

    public Camera(Activity activity){
        mActivity = activity;
    }

    public void takePhoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mCapturePicUri = Uri.fromFile(getOutputImageFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturePicUri);
        mActivity.startActivityForResult(intent, REQUEST_FOR_CAPTURE_PIC);
    }

    public void takeVedio(){
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mCaptureVedioUri = Uri.fromFile(getOutputVedioFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCaptureVedioUri);
        mActivity.startActivityForResult(intent, REQUEST_FOR_CAPTURE_VEDIO);
    }

    private File getOutputImageFile(){
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "emrpad");

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    }

    private File getOutputVedioFile(){
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "emrpad");

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            return null;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "MOVIE_" + timeStamp + ".mp4");
    }

    public Uri getCapturePicUri(){
        return mCapturePicUri;
    }
    public Uri getCaptureVedioUri(){
        return mCaptureVedioUri;
    }
}
