package uk.ordere.launcher.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.mukesh.OnOtpCompletionListener;
import com.mukesh.OtpView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import uk.ordere.launcher.R;

public class PinCodeActivity extends AppCompatActivity {
    private OtpView otpView;
    private CardView cardViewInvalidPin;

    // To keep track of activity's window focus
    boolean currentFocus;

    // To keep track of activity's foreground/background status
    boolean isPaused;

    Handler collapseNotification;
    boolean isUnlock = false;


//    private Button validate_button;
    private static final String TAG = "PinCodeActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_code);

        Intent routeIntent = getIntent();
        String route = routeIntent.getStringExtra("route");


        Log.e("ROUTEE____",route);
        cardViewInvalidPin = findViewById(R.id.invalid_pin);
        otpView = findViewById(R.id.otp_view);
//        validate_button = findViewById(R.id.validate_button);
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String[] parts = date.split("-");
        String part1=new StringBuilder(parts[1]).reverse().toString();
        String part2=new StringBuilder(parts[2]).reverse().toString();
        String mid = "23";

        int add = Integer.parseInt(part1)+Integer.parseInt(part2);

        if(add%2==0){
            mid="12";
        }
        String dateFinal=part2+mid+part1;




        otpView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                cardViewInvalidPin.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        otpView.setOtpCompletionListener(otp -> {

            if (otp.equals(dateFinal)){
                isUnlock = true;
                Intent intent;
                if(route.equals("default_launcher")){


                    intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);


                }else{
                    intent = new Intent(Settings.ACTION_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                }

                startActivity(intent);
                finish();
            }else{

                otpView.setText("");
                cardViewInvalidPin.setVisibility(View.VISIBLE);
            }



        });




    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(ACTIVITY_SERVICE);

        if(!isUnlock){
            activityManager.moveTaskToFront(getTaskId(), 0);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        isUnlock = false;
    }
}