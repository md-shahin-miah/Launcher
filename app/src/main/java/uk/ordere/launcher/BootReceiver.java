package uk.ordere.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ordere.launcher.activity.HomeActivity;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, HomeActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }
}