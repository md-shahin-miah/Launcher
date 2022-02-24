package uk.ordere.launcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import uk.ordere.launcher.activity.HomeActivity
import uk.ordere.launcher.activity.HomeActivity.Companion.isnotify

class MyFirebaseMessagingService : FirebaseMessagingService() {


    private val TAG = "FireBaseMessagingService"
    var NOTIFICATION_CHANNEL_ID = "net.larntech.notification"
    val NOTIFICATION_ID = 100

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.e("message","Message Received ...");

        if (remoteMessage.data.size > 0) {
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            showNotification(applicationContext, title, body)
        } else {
            val title = remoteMessage.notification!!.title
            val body = remoteMessage.notification!!.body
            showNotification(applicationContext, title, body)
        }
    }


    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.e("token","New Token")
    }


    fun showNotification(
        context: Context,
        title: String?,
        message: String?
    ) {

        Log.e("MESSAGE________","HELLO ")

        val intent = Intent(context, HomeActivity::class.java)
       // intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("app_package",message)
        startActivity(intent)
        isnotify = true;


//        val ii: Intent
//        ii = Intent(context, HomeActivity::class.java)
//        ii.data = Uri.parse("custom://" + System.currentTimeMillis())
//        ii.action = "actionstring" + System.currentTimeMillis()
//        ii.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
//        val pi =
//            PendingIntent.getActivity(context, 0, ii, PendingIntent.FLAG_UPDATE_CURRENT)
//        val notification: Notification
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            //Log.e("Notification", "Created in up to orio OS device");
//            notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setOngoing(true)
//                .setSmallIcon(getNotificationIcon())
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setContentIntent(pi)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .setWhen(System.currentTimeMillis())
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                .setContentTitle(title).build()
//            val notificationManager = context.getSystemService(
//                Context.NOTIFICATION_SERVICE
//            ) as NotificationManager
//            val notificationChannel = NotificationChannel(
//                NOTIFICATION_CHANNEL_ID,
//                title,
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            notificationManager.createNotificationChannel(notificationChannel)
//            notificationManager.notify(NOTIFICATION_ID, notification)
//        } else {
//            notification = NotificationCompat.Builder(context)
//                .setSmallIcon(getNotificationIcon())
//                .setAutoCancel(true)
//                .setContentText(message)
//                .setContentIntent(pi)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                .setContentTitle(title).build()
//            val notificationManager = context.getSystemService(
//                Context.NOTIFICATION_SERVICE
//            ) as NotificationManager
//            notificationManager.notify(NOTIFICATION_ID, notification)
//        }
    }

    private fun getNotificationIcon(): Int {
        val useWhiteIcon =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        return if (useWhiteIcon) R.mipmap.ic_launcher else R.mipmap.ic_launcher
    }

}