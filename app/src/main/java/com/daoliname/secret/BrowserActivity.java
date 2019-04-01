package com.daoliname.secret;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.daoliname.secret.utils.QRCodeUtil;

public class BrowserActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String URL_KEY = "url_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        findViewById(R.id.ib_close).setOnClickListener(this);
        Intent intent = getIntent();
        String stringExtra = null;
        if (intent != null) {
            stringExtra = intent.getStringExtra(URL_KEY);
        }
        if (!QRCodeUtil.isTextUri(stringExtra)) {
            Toast.makeText(this, "invalid url.", Toast.LENGTH_LONG).show();
            this.finish();
        }
        ((WebView) findViewById(R.id.web_view)).loadUrl(stringExtra);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_close:
                this.finish();
                break;
            default:
                break;
        }
    }
}
