// MainActivity.java  (apne package ka naam sabse upar wali line me rakhna)
package com.example.ghar3d;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    EditText serverUrl, sizeFt;
    WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        serverUrl = new EditText(this);
        serverUrl.setHint("Server URL, jaise http://192.168.1.5:5000");
        serverUrl.setText("http://192.168.1.5:5000");
        sizeFt = new EditText(this);
        sizeFt.setHint("Ghar ki sabse badi side (feet)");
        Button pick = new Button(this);
        pick.setText("Ghar ki photo chuno");
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);

        root.addView(serverUrl);
        root.addView(sizeFt);
        root.addView(pick);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 1);
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && res == RESULT_OK && data != null) upload(data.getData());
    }

    void upload(Uri uri) {
        final String base = serverUrl.getText().toString().trim();
        final String size = sizeFt.getText().toString().trim();
        Toast.makeText(this, "Analyse ho raha hai... 1-2 minute lag sakte hain", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                String bd = "----ghar" + System.currentTimeMillis();
                HttpURLConnection c = (HttpURLConnection) new URL(base + "/upload").openConnection();
                c.setDoOutput(true);
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(600000);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + bd);
                DataOutputStream o = new DataOutputStream(c.getOutputStream());
                o.writeBytes("--" + bd + "\r\nContent-Disposition: form-data; name=\"size_ft\"\r\n\r\n" + size + "\r\n");
                o.writeBytes("--" + bd + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"a.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n");
                InputStream in = getContentResolver().openInputStream(uri);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                in.close();
                o.writeBytes("\r\n--" + bd + "--\r\n");
                o.flush();
                final int code = c.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) web.loadUrl(base + "/view");
                    else Toast.makeText(this, "Server error: " + code, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
