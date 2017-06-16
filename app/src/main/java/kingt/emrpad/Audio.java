package kingt.emrpad;

import android.media.MediaRecorder;

/**
 * Created by shao on 2017/5/10.
 */

public class Audio {
    private Boolean starting = false;
    private MediaRecorder recorder;
    private String outputFile;

    public void start() throws Exception {
        if (!starting) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(outputFile);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
                recorder.start();

                starting = true;
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public void stop() {
        if (starting) {
            recorder.stop();
            recorder.release();
            starting = false;
            recorder = null;
        }
    }

    public Boolean isStarting() {
        return starting;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }
}
