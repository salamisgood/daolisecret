package com.daoliname.secret;

import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.daoliname.secret.utils.QRCodeUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        String encryptInfo = stringFromJNI();
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(encryptInfo);
        Bitmap qrCodeBitmap = QRCodeUtil.createQRCodeBitmap(encryptInfo, 100, 100);
        ((ImageView)findViewById(R.id.iv_qr_code)).setImageBitmap(qrCodeBitmap);

        String path = MediaStore.Images.Media.insertImage(getContentResolver(), qrCodeBitmap, "", "");
        Log.d(TAG,"path = " + path);
        msc.connect();
    }

    final MediaScannerConnection msc = new MediaScannerConnection(MainActivity.this, new MediaScannerConnection.MediaScannerConnectionClient() {

        public void onMediaScannerConnected() {
            msc.scanFile(Environment.getExternalStorageDirectory().getPath()+"image.jpg", "image/jpeg");
        }

        @Override
        public void onScanCompleted(String s, Uri uri) {
            msc.disconnect();
        }

    });

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
