package com.daoliname.secret;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.daoliname.secret.utils.QRCodeUtil;

public class MainActivity extends AppCompatActivity {

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

        // TODO: 2019/3/28 0028 save bitmap image to sdcard.

        
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
