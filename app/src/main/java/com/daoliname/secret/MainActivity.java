package com.daoliname.secret;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.daoliname.secret.models.RegisterBody;
import com.daoliname.secret.models.Response;
import com.daoliname.secret.utils.QRCodeUtil;
import com.google.gson.Gson;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.MediaType;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST = 0x19;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private Handler mUIHandler = new UIHandler();
    ProgressDialog mProgressDialog;
    private String mUserId = null;
    private static final int UPLOAD_SUCCESS = 0x100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_gen).setOnClickListener(this);
        findViewById(R.id.btn_read).setOnClickListener(this);

        registerPhone();
    }

    private void registerPhone() {
        // getPhoneIMEI
        showProgressDialog();
        OkHttpUtils
                .postString()
                .url(RequestConstants.base_url + RequestConstants.register)
                .mediaType(MediaType.parse("application/json; charset=utf-8"))
                .content(new Gson().toJson(new RegisterBody("gaominghui")))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        mProgressDialog.cancel();
                        showToast(e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        mProgressDialog.cancel();
                        if (response == null || response.isEmpty()) {
                            showToast("response is empty.", Toast.LENGTH_SHORT);
                            return;
                        }
                        Response responseBean = new Gson().fromJson(response, Response.class);
                        if (responseBean.isSuccess()){
                            mUserId = responseBean.getId();
                            Log.d(TAG, "mUserId -> " + mUserId);
                        } else {
                            showToast("register failed.", Toast.LENGTH_SHORT);
                        }
                    }
                });
    }

    private void showToast(String localizedMessage, int lengthShort) {
        Toast.makeText(MainActivity.this, localizedMessage, lengthShort).show();
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("正在注册...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String szImei = TelephonyMgr.getDeviceId();
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
                inputContent = encryptContent(inputContent);

                showProgressDialog();
                // save to cache
                String cachePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
                File textInputCacheFile = new File(cachePath + File.separator +System.currentTimeMillis() + ".html");
                BufferedWriter bw = null;
                FileWriter fw = null;
                try {
                    fw = new FileWriter(textInputCacheFile);
                    bw = new BufferedWriter(fw);
                    bw.write(inputContent);
                    bw.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bw != null) bw.close();
                        if (fw != null) fw.close();
                    } catch (IOException e) {
                        Log.e(TAG, "do nothing...");
                    }
                }
                uploadText(textInputCacheFile);
                break;
            case R.id.btn_read:
                // read the weixin micromsg dir
                String wxPicPath = Environment.getExternalStorageDirectory().getPath() + "/Tencent/MicroMsg/WeiXin";
                File wxFolder = new File(wxPicPath);
                File[] files = wxFolder.listFiles();
                if (files == null || files.length == 0) {
                    showToast("Tencent/MicroMsg/WeiXin is empty.", Toast.LENGTH_LONG);
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
                    showToast("last file is not jpg.", Toast.LENGTH_SHORT);
                    break;
                }
                // recgonize qrcode image
                Bitmap bitmap = BitmapFactory.decodeFile(latestFile.getAbsolutePath());
                String recUrl = QRCodeUtil.recogQRcode(bitmap);
                //String decryptContent = decryptContent(resInfo);
                //resInfo = "http://www.baidu.com";
                if (QRCodeUtil.isTextUri(recUrl)) {
                    Intent intent = new Intent(this, WebActivity.class);
                    intent.putExtra(WebActivity.URL_KEY, recUrl);
                    this.startActivity(intent);
                } else {
                    Log.e(TAG, recUrl + " is not a valid url.");
                    showToast(recUrl + "is not a valid url.", Toast.LENGTH_SHORT);
                    // get content from server
                    OkHttpUtils.get().url(recUrl).build().execute(new StringCallback() {
                        @Override
                        public void onError(Call call, Exception e, int id) {
                            Log.d(TAG, "onError: " + e.getLocalizedMessage());
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Log.d(TAG, "onResponse: " + response);
                            response = decryptContent(response);
                            ((EditText) findViewById(R.id.et_content)).setText(response);

                        }
                    });
                }
                break;
            default:
                break;
        }
    }

    private void uploadText(File textFilePlain) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "multipart/form-data");

        OkHttpUtils.post()//
                //.addFile("mFile", "messenger_01.png", file)//
                .addFile("file", textFilePlain.getName(), textFilePlain)//
                .url(RequestConstants.base_url + RequestConstants.upload)
                .headers(headers)//
                .build()//
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        mProgressDialog.cancel();
                        showToast(e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        mProgressDialog.cancel();
                        if (response == null || response.isEmpty()){
                            showToast("response is empty.", Toast.LENGTH_SHORT);
                            return;
                        }
                        Response responseBean = new Gson().fromJson(response, Response.class);
                        Message msg = Message.obtain();
                        msg.what = UPLOAD_SUCCESS;
                        if (responseBean.isSuccess()){
                            String url = responseBean.getUrl();
                            Log.d(TAG, "url -> " + url);
                            msg.obj = RequestConstants.base_url + url;
                        } else {
                            msg.obj = ((EditText) findViewById(R.id.et_content)).getText().toString();
                        }
                        mUIHandler.sendMessage(msg);
                    }
                });
    }

    private String encryptContent(String inputContent) {
        // todo encryptContent
        String base64EncodedStr = Base64.encodeToString(inputContent.getBytes(), Base64.DEFAULT);
        return base64EncodedStr;
    }

    private String decryptContent(String content) {
        byte[] decode = Base64.decode(content, Base64.DEFAULT);
        String base64DecodeStr = new String(decode);
        return base64DecodeStr;
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPLOAD_SUCCESS:
                    String encryptInfo = (String) msg.obj;

                    // generate qrcode and save to dicm
                    Bitmap qrCodeBitmap = QRCodeUtil.createQRCodeBitmap(encryptInfo, 300, 300);
                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), qrCodeBitmap, "", "");
                    Log.d(TAG, "path = " + path);
                    msc.connect();
                    showToast("生成二维码已经存到" + path, Toast.LENGTH_SHORT);
                    break;
                default:
                    break;
            }
        }
    }
}
