package com.example.downloadcatalogues;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private final MutableLiveData<Integer> progress = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        N.B. there's two ways to download:
        1- IOStream: old way, not recommended
        2- DownloadManager: currently recommended way
         */

        findViewById(R.id.extrauma_button).setOnClickListener(v -> {
            String urlZipFile = "https://edetailing.minapharm.com/ed/EXT_DNA_GE_1.zip";
//            downloadWithDownloadManager(urlZipFile, "extrauma");
            downloadWithIOStream(urlZipFile, "extrauma");
        });

        findViewById(R.id.bonone_button).setOnClickListener(v -> {
            String urlZipFile = "http://edetailing.minapharm.com/ed/BON_ONE25_1.zip";
//            downloadWithDownloadManager(urlZipFile, "bonone");
            downloadWithIOStream(urlZipFile, "bonone");
        });

        findViewById(R.id.ophtatrov_button).setOnClickListener(v -> {
            String urlZipFile = "https://edetailing.minapharm.com/ed/Ophtatrov_1.zip";
            downloadWithDownloadManager(urlZipFile, "ophtatrov");
//            downloadWithIOStream(urlZipFile, "ophtatrov");
        });
    }

    private void downloadWithDownloadManager(String url, String title) {
        DownloadManager downloadManager;

        try {
            File file = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/test/", "123.zip");

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(title)// Title of the Download Notification
                    .setDescription("Downloading")// Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                    .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                    // .setRequiresCharging(false)// Set if charging is required to begin the download
                    .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

            downloadManager = (DownloadManager) getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
            long downloadID = downloadManager.enqueue(request);
            Log.v("downloadProId", downloadID + "");
        } catch (Exception e) {
            Log.w("stopped", e.toString());
        }
    }

    private void downloadWithIOStream (String url, String fileName){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        executor.execute(() -> {

            int count;

            try {
                URL url1 = new URL(url);
                int lengthOfFile;
                // https
                if (url.contains("https")) {
                    //--------------- HANDLE SSLHandshakeException ---------------//
                    try {
                        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
                        SSLContext context = SSLContext.getInstance("TLS");
                        context.init(null, new X509TrustManager[]{new X509TrustManager(){
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }}}, new SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //-----------------------------------------------------------//
                    HttpsURLConnection connection = (HttpsURLConnection) url1.openConnection();
                    connection.connect();
                    lengthOfFile = connection.getContentLength();
                }
                // http
                else {
                    URLConnection connection = url1.openConnection();
                    connection.connect();
                    lengthOfFile = connection.getContentLength();
                }

                InputStream input = new BufferedInputStream(url1.openStream());
                OutputStream output = new FileOutputStream(getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/ "+fileName+" /");

                byte[] data = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    progress.postValue((int) ((total * 100) / lengthOfFile));
                    output.write(data, 0, count);
                }
                output.close();
                input.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                mProgressDialog.dismiss();
            }
        });

        handler.post(() -> {
            progress.observe(this, progress -> {
                mProgressDialog.setProgress(progress);
                if (progress == 100)
                    mProgressDialog.dismiss();
            });
        });
    }
}