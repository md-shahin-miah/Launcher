package uk.ordere.launcher.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.multidex.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import uk.ordere.launcher.R;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.ContentValues.TAG;

public class AppUpdateActivity extends AppCompatActivity {

    ImageView updateMerchantApp,updateMerchantApp2, updateLauncher, updateWalletApp, deleteMerchantApp,deleteMerchantApp2, deleteWalletApp;
    RelativeLayout layout_merchant_app2;
    Toolbar toolbar;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    //    private static final String URl = "https://github.com/faithonline002/app/raw/main/ordere_launcher.apk";
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    boolean isInstallTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_update);

        updateMerchantApp = findViewById(R.id.ic_ol);
        updateMerchantApp2 = findViewById(R.id.ic_ol2);
        updateLauncher = findViewById(R.id.ic_om_launcher);
        updateWalletApp = findViewById(R.id.ic_om_wallet);

        deleteMerchantApp = findViewById(R.id.ic_delete_merchant);
        deleteMerchantApp2 = findViewById(R.id.ic_delete_merchant2);
        deleteWalletApp = findViewById(R.id.ic_om_wallet_delete);
        layout_merchant_app2=findViewById(R.id.layout_merchant_app2);

        toolbar = findViewById(R.id.toolBar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


//        "https://github.com/faithonline002/app/raw/main/merchant.apk"
        checkPermissionRead();


        updateMerchantApp.setOnClickListener(v -> {
            isInstallTriggered = true;
            if (checkPermission()) {
                new DownloadNewVersion("https://superadmin.ordere.co.uk/api/get_merchant_app", "merchant.apk", "merchant.apk").execute();
            } else {
                requestPermission();
            }
        });

        if (isAppInstalled(this,"uk.co.ordervox.merchant2")){
            layout_merchant_app2.setVisibility(View.VISIBLE);
        }
        else {
            layout_merchant_app2.setVisibility(View.GONE);

        }

        updateMerchantApp2.setOnClickListener(v -> {
            isInstallTriggered = true;
            if (checkPermission()) {
                new DownloadNewVersion("https://superadmin.ordere.co.uk/api/get_merchant_app2", "merchant2.apk", "merchant2.apk").execute();
            } else {
                requestPermission();
            }
        });
        updateLauncher.setOnClickListener(v -> {
            isInstallTriggered = true;
            if (checkPermission()) {
                new DownloadNewVersion("https://superadmin.ordere.co.uk/api/get_launcher_app", "launcher.apk", "launcher.apk").execute();
            } else {
                requestPermission();
            }
        });
        updateWalletApp.setOnClickListener(v -> {
            isInstallTriggered = true;
            if (checkPermission()) {
                new DownloadNewVersion("https://superadmin.ordere.co.uk/api/get_wallet_app", "wallet.apk", "wallet.apk").execute();
            } else {
                requestPermission();
            }
        });

        if (isAppInstalled(this, "uk.co.ordere.ordere_launcher")) {
            updateLauncher.setImageResource(R.drawable.ic_baseline_update_24);

        } else {
            updateLauncher.setImageResource(R.drawable.ic_baseline_download_24);

        }

        if (isAppInstalled(this, "uk.co.ordervox.merchant")) {
            deleteMerchantApp.setVisibility(View.VISIBLE);

            updateMerchantApp.setImageResource(R.drawable.ic_baseline_update_24);

            deleteMerchantApp.setOnClickListener(v -> {
                deleteMerchantApp.setVisibility(View.INVISIBLE);
                if (isAppInstalled(this, "uk.co.ordervox.merchant")) {
                    isInstallTriggered = true;
                    uninstallApp("uk.co.ordervox.merchant");
                }

            });

        } else {
            updateMerchantApp.setImageResource(R.drawable.ic_baseline_download_24);
            deleteMerchantApp.setVisibility(View.INVISIBLE);

        }

        if (isAppInstalled(this, "uk.co.ordervox.merchant2")) {
            deleteMerchantApp2.setVisibility(View.VISIBLE);

            updateMerchantApp2.setImageResource(R.drawable.ic_baseline_update_24);

            deleteMerchantApp2.setOnClickListener(v -> {
                deleteMerchantApp2.setVisibility(View.INVISIBLE);
                if (isAppInstalled(this, "uk.co.ordervox.merchant2")) {
                    isInstallTriggered = true;
                    uninstallApp("uk.co.ordervox.merchant2");
                }

            });

        } else {
            updateMerchantApp2.setImageResource(R.drawable.ic_baseline_download_24);
            deleteMerchantApp2.setVisibility(View.INVISIBLE);

        }
        if (isAppInstalled(this, "uk.co.ordere.merchant_wallet")) {
            deleteWalletApp.setVisibility(View.VISIBLE);
            updateWalletApp.setImageResource(R.drawable.ic_baseline_update_24);
            deleteWalletApp.setOnClickListener(v -> {
                deleteWalletApp.setVisibility(View.INVISIBLE);
                if (isAppInstalled(this, "uk.co.ordere.merchant_wallet")) {
                    isInstallTriggered = true;
                    uninstallApp("uk.co.ordere.merchant_wallet");
                }
            });

        } else {
            updateWalletApp.setImageResource(R.drawable.ic_baseline_download_24);
            deleteWalletApp.setVisibility(View.INVISIBLE);

        }

    }

    private void uninstallApp(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "App is not installed", Toast.LENGTH_SHORT).show();
        }

    }

    private void checkPermissionRead() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

    }


    private void installApkProgrammatically(String getPathName) {


        try {
            File path = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

            File file = new File(path, getPathName);

            Uri uri;

            if (file.exists()) {

                Intent unKnownSourceIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", AppUpdateActivity.this.getPackageName())));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    if (!AppUpdateActivity.this.getPackageManager().canRequestPackageInstalls()) {
                        startActivityForResult(unKnownSourceIntent, 0);
                    } else {

                        Uri fileUri = FileProvider.getUriForFile(AppUpdateActivity.this.getBaseContext(), AppUpdateActivity.this.getApplicationContext().getPackageName() + ".provider", file);
                        Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
                        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                        intent.setDataAndType(fileUri, "application/vnd.android" + ".package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
//                        alertDialog.dismiss();
                    }

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    Intent intent1 = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    uri = FileProvider.getUriForFile(AppUpdateActivity.this.getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);
                    AppUpdateActivity.this.grantUriPermission("com.abcd.xyz", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    AppUpdateActivity.this.grantUriPermission("com.abcd.xyz", uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent1.setDataAndType(uri,
                            "application/*");
                    intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent1.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(intent1);

                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    uri = Uri.fromFile(file);

                    intent.setDataAndType(uri,
                            "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } else {

                Log.i(TAG, " file " + file.getPath() + " does not exist");
            }
        } catch (Exception e) {

            Log.i(TAG, "" + e.getMessage());

        }
    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
    }


    class DownloadNewVersion extends AsyncTask<String, Integer, Boolean> {

        ProgressDialog bar;
        String UrlForDownLoad;
        String writeApkDownloadName;
        String readApkName;

        public DownloadNewVersion(String uRl, String writeApkDownloadName, String readApkName) {

            this.UrlForDownLoad = uRl;
            this.writeApkDownloadName = writeApkDownloadName;
            this.readApkName = readApkName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar = new ProgressDialog(AppUpdateActivity.this);
            bar.setCancelable(false);
            bar.setMessage("Downloading...");
            bar.setIndeterminate(true);
            bar.setCanceledOnTouchOutside(false);
            bar.show();
        }

        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            bar.setIndeterminate(false);
            bar.setMax(100);
            bar.setProgress(progress[0]);
            String msg = "";
            if (progress[0] > 99) {
                msg = "Downloading...  ";
            } else {
//                msg="Downloading... "+progress[0]+"%";
                msg = "Downloading... ";
            }
            bar.setMessage(msg);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            bar.dismiss();
            if (result) {
                Toast.makeText(AppUpdateActivity.this, "Done!!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AppUpdateActivity.this, "Error: Try Again", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            Boolean flag = false;
            try {
                URL url = new URL(UrlForDownLoad);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setDoOutput(false);
                c.connect();
                File PATH = AppUpdateActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(PATH.getAbsolutePath());
                file.mkdirs();
                File outputFile = new File(file, writeApkDownloadName);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                FileOutputStream fos = new FileOutputStream(outputFile);
                InputStream is = c.getInputStream();
                int total_size = 4581692;//size of apk
                byte[] buffer = new byte[1024];
                int len1 = 0;
                int per = 0;
                int downloaded = 0;
                while ((len1 = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len1);
                    downloaded += len1;
                    per = (int) (downloaded * 100 / total_size);
                    publishProgress(per);
                }
                fos.close();
                is.close();
                installApkProgrammatically(readApkName);
//                OpenNewVersion(PATH);
                flag = true;
            } catch (Exception e) {
                Log.e(TAG, "Update Error: " + e.getMessage());
                e.printStackTrace();
                flag = false;
            }
            return flag;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);
        if (!isInstallTriggered) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }


    }

    @Override
    public boolean onSupportNavigateUp() {

        onBackPressed();
        return true;
    }
//
//    public static boolean isAppInstalled(Context context, String packageName) {
//        try {
//            context.getPackageManager().getApplicationInfo(packageName, 0);
//            return true;
//        }
//        catch (PackageManager.NameNotFoundException e) {
//            return false;
//        }
//    }

    public static boolean isAppInstalled(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return false;
        }
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (isAppInstalled(this, "uk.co.ordervox.merchant")) {
            deleteMerchantApp.setVisibility(View.VISIBLE);
            updateMerchantApp.setImageResource(R.drawable.ic_baseline_update_24);
        } else {
            deleteMerchantApp.setVisibility(View.INVISIBLE);
            updateMerchantApp.setImageResource(R.drawable.ic_baseline_download_24);
        }
        if (isAppInstalled(this, "uk.co.ordervox.merchant2")) {
            deleteMerchantApp2.setVisibility(View.VISIBLE);
            updateMerchantApp2.setImageResource(R.drawable.ic_baseline_update_24);
        } else {
            deleteMerchantApp2.setVisibility(View.INVISIBLE);
            updateMerchantApp2.setImageResource(R.drawable.ic_baseline_download_24);
        }

        if (isAppInstalled(this, "uk.co.ordere.merchant_wallet")) {
            updateWalletApp.setImageResource(R.drawable.ic_baseline_update_24);
            deleteWalletApp.setVisibility(View.VISIBLE);
        } else {
            deleteWalletApp.setVisibility(View.INVISIBLE);
            updateWalletApp.setImageResource(R.drawable.ic_baseline_download_24);
        }


        if (isAppInstalled(this,"uk.co.ordervox.merchant2")){
            layout_merchant_app2.setVisibility(View.VISIBLE);
        }
        else {
            layout_merchant_app2.setVisibility(View.GONE);

        }


    }
}