package uk.co.ordervox.myapplication

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class DownloadController(private val context: Context, private val url: String) {

    companion object {

        const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"

    }

    fun enqueueDownload(title:String) {


        var destination = "/storage/emulated/0/Download/"
        destination += title+".apk"

        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        Log.e("APP_DOWNLOAD___",destination)

        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
       request.setMimeType(MIME_TYPE)
        request.setTitle(title)

//
//        // set destination
        request.setDestinationUri(uri)

        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        Toast.makeText(context, "downloading..", Toast.LENGTH_LONG)
                .show()


    }

     fun  showInstallOption(
            destination: String,
            uri: Uri
    ) {




        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                    context: Context,
                    intent: Intent
            ) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    val contentUri = FileProvider.getUriForFile(
//                            context,
//                            "ordere_laucher_inappupdate",
//                            File(destination)
//                    )
//                    val install = Intent(Intent.ACTION_VIEW)
//                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
//                    install.data = contentUri
//                    context.startActivity(install)
//                    context.unregisterReceiver(this)
//                    // finish()
//                } else {
//                    val install = Intent(Intent.ACTION_VIEW)
//                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    install.setDataAndType(
//                            uri,
//                            APP_INSTALL_PATH
//                    )
//                    context.startActivity(install)
//                    context.unregisterReceiver(this)
//                    // finish()
//                }

                Handler(Looper.getMainLooper()).postDelayed({

                    Log.e("HANDLER","CALLED");
                    val toInstall = File(destination)
                    val intent: Intent
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val apkUri = FileProvider.getUriForFile(context,  "ordere_laucher_inappupdate", toInstall)
                        intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                        intent.data = apkUri
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    } else {
                        val apkUri = Uri.fromFile(toInstall)
                        intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)

                }, 60000)


            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}