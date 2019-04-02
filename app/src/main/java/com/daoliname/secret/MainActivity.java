package com.daoliname.secret;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.daoliname.secret.utils.QRCodeUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.datatype.Duration;

import okio.Options;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_gen).setOnClickListener(this);
        findViewById(R.id.btn_read).setOnClickListener(this);
    }

    final MediaScannerConnection msc = new MediaScannerConnection(MainActivity.this,
            new MediaScannerConnection.MediaScannerConnectionClient() {

        public void onMediaScannerConnected() {
            msc.scanFile(Environment.getExternalStorageDirectory().getPath() + "image.jpg", "image/jpeg");
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gen:
                Editable text = ((EditText) findViewById(R.id.et_content)).getText();
                String inputContent = text.toString();
                // encrypted
                String encryptInfo = encryptContent(inputContent);
                // generate qrcode and save to dicm
                Bitmap qrCodeBitmap = QRCodeUtil.createQRCodeBitmap(encryptInfo, 300, 300);
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), qrCodeBitmap, "", "");
                Log.d(TAG, "path = " + path);
                msc.connect();
                break;
            case R.id.btn_read:
                // read the weixin micromsg dir
                String wxPicPath = Environment.getExternalStorageDirectory().getPath() + "/Tencent/MicroMsg/WeiXin";
                File wxFolder = new File(wxPicPath);
                File[] files = wxFolder.listFiles();
                if (files == null || files.length == 0) {
                    Toast.makeText(this, "Tencent/MicroMsg/WeiXin is empty.", Toast.LENGTH_LONG).show();
                    break;
                }
                // wx_camera_1526390654692.jpg get the max timestamp
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        return (int) (file2.lastModified() - file1.lastModified());
                    }
                });

                File latestFile = files[0];
                if (!latestFile.getName().contains("jpg")) {
                    Toast.makeText(this, "last file is not jpg.", Toast.LENGTH_SHORT).show();
                    break;
                }
                // recgonize qrcode image
                Bitmap bitmap = BitmapFactory.decodeFile(latestFile.getAbsolutePath());
                String resInfo = QRCodeUtil.recogQRcode(bitmap);
                resInfo = "http://www.baidu.com";
                if (QRCodeUtil.isTextUri(resInfo)){
                    Intent intent = new Intent(this,WebActivity.class);
                    intent.putExtra(WebActivity.URL_KEY,resInfo);
                    this.startActivity(intent);
                } else  {
                    Log.e(TAG,resInfo + " is not a valid url.");
                }
                break;
            default:
                break;
        }
    }

    private String encryptContent(String inputContent) {
        // todo encryptContent
        return inputContent;
    }
}
