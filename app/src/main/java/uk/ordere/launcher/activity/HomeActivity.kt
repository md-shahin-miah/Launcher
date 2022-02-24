package uk.ordere.launcher.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Service
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.home.version_id_tv
import kotlinx.android.synthetic.main.home_layout.*
import uk.co.ordervox.myapplication.DownloadController
import uk.ordere.launcher.R
import uk.ordere.launcher.UIObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.concurrent.fixedRateTimer


class HomeActivity: UIObject, AppCompatActivity(),
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {




    private val _grantedPermissions = HashSet<String>()
    private val _permRequestCode = 123
    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
        var isnotify = false


    }



    // To keep track of activity's window focus
    var currentFocus = false

    // To keep track of activity's foreground/background status
    var isPaused = false

    var collapseNotificationHandler: Handler? = null

    lateinit var downloadController: DownloadController
    private var bufferedPointerCount = 1 // how many fingers on screen
    private var pointerBufferTimer = Timer()
   var isMerchant = false
    private lateinit var mDetector: GestureDetectorCompat

    // timers
    private var clockTimer = Timer()
    private var tooltipTimer = Timer()

    private var settingsIconShown = false

    var mybroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {

            } else if (intent.action == Intent.ACTION_SCREEN_OFF) {
                // Also show lock-screen, to remove flicker/delay when screen on ?

            }
        }


    }




    fun preventStatusBarExpansion(context: Context) {
        val manager = context.applicationContext
            .getSystemService(WINDOW_SERVICE) as WindowManager
        val activity = context as Activity
        val localLayoutParams = WindowManager.LayoutParams()

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){

            localLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        }else{
            localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }

        localLayoutParams.gravity = Gravity.TOP
        localLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // this is to enable the notification to recieve touch events
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or  // Draws over status bar
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        val resId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        var result = 0
        if (resId > 0) {
            result = activity.resources.getDimensionPixelSize(resId)
        }
        localLayoutParams.height = result
        localLayoutParams.format = PixelFormat.TRANSPARENT
        val view = customViewGroup(context)
        manager.addView(view, localLayoutParams)
    }

    class customViewGroup(context: Context?) : ViewGroup(context) {
        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            Log.v("customViewGroup", "**********Intercepted")
            return true
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MESSAGE________", "onCreate ")








//        if (Build.VERSION.SDK_INT >= 23) {
//            if (!Settings.canDrawOverlays(this@HomeActivity)) {
//                val intent = Intent(
//                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:$packageName")
//                )
//                startActivityForResult(intent, 1234)
//            }
//        } else {
//            val intent = Intent(this@HomeActivity, Service::class.java)
//            startService(intent)
//        }


        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(this@HomeActivity)) {

                    preventStatusBarExpansion(this)

                }
            }else{
                preventStatusBarExpansion(this)
            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }
















        // Preload apps to speed up the Apps Recycler

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        // Initialise layout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.home_layout)
        if(!isAppInstalled("uk.co.ordere.merchant_wallet")){
            walletApp.visibility = View.GONE
        }

        if(!isAppInstalled("uk.co.ordervox.merchant")){
            merchantApp.visibility = View.GONE
        }
        if(!isAppInstalled("uk.co.ordervox.merchant2")){
            merchantApp2.visibility = View.GONE
        }

        registerReceiver(mybroadcast, IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(mybroadcast, IntentFilter(Intent.ACTION_SCREEN_OFF));


        try{ showCustomPopupMenu()
        }catch (e:Exception){

        }


        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)

        version_id_tv.text="version code-"+info.versionName.toString()



        settings.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@HomeActivity, SettingActivity::class.java)
            startActivity(intent)
        })

        updateApps.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@HomeActivity, AppUpdateActivity::class.java)
            startActivity(intent)
        })

        if(isAppInstalled("uk.co.ordervox.merchant")){
            openApp("uk.co.ordervox.merchant")
        }

//

        checkPermission()

    }








     override fun onStart(){
        super<AppCompatActivity>.onStart()
        mDetector = GestureDetectorCompat(this, this)
        mDetector.setOnDoubleTapListener(this)


        val keyguardManager = this.getSystemService(KEYGUARD_SERVICE) as KeyguardManager?
        val lock = keyguardManager!!.newKeyguardLock(KEYGUARD_SERVICE)
        lock.disableKeyguard()

        super<UIObject>.onStart()


    }




    override fun onWindowFocusChanged(hasFocus: Boolean) {
        currentFocus = hasFocus
        if (!hasFocus) {

            // Method that handles loss of window focus
            collapseNow()
        }


    }


    private fun showCustomPopupMenu() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // View view = layoutInflater.inflate(R.layout.dummy_layout, null);
        val valetModeWindow = View.inflate(this, R.layout.dummy_layout, null) as ViewGroup
        val LAYOUT_FLAG: Int
        LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.CENTER or Gravity.CENTER
        params.x = 0
        params.y = 0
        windowManager.addView(valetModeWindow, params)
    }




    fun collapseNow() {

        // Initialize 'collapseNotificationHandler'
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = Handler()
        }

        // If window focus has been lost && activity is not in a paused state
        // Its a valid check because showing of notification panel
        // steals the focus from current activity's window, but does not
        // 'pause' the activity
        if (!currentFocus && !isPaused) {

            // Post a Runnable with some delay - currently set to 300 ms
            collapseNotificationHandler!!.postDelayed(object : Runnable {
                @SuppressLint("WrongConstant")
                override fun run() {

                    // Use reflection to trigger a method from 'StatusBarManager'
                    val statusBarService = getSystemService("statusbar")
                    var statusBarManager: Class<*>? = null
                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager")
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    }
                    var collapseStatusBar: Method? = null
                    try {

                        // Prior to API 17, the method to call is 'collapse()'
                        // API 17 onwards, the method to call is `collapsePanels()`
                        collapseStatusBar = if (Build.VERSION.SDK_INT > 16) {
                            statusBarManager!!.getMethod("collapsePanels")
                        } else {
                            statusBarManager!!.getMethod("collapse")
                        }
                    } catch (e: NoSuchMethodException) {
                        e.printStackTrace()
                    }
                    collapseStatusBar!!.setAccessible(true)
                    try {
                        collapseStatusBar!!.invoke(statusBarService)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    }

                    // Check if the window focus has been returned
                    // If it hasn't been returned, post this Runnable again
                    // Currently, the delay is 100 ms. You can change this
                    // value to suit your needs.
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler!!.postDelayed(this, 100L)
                    }
                }
            }, 300L)
        }
    }

    override fun onResume() {
        super.onResume()

        isPaused = false


        if(!isAppInstalled("uk.co.ordere.merchant_wallet")){
            walletApp.visibility = View.GONE
        }

        if(!isAppInstalled("uk.co.ordervox.merchant")){
            merchantApp.visibility = View.GONE
        }
        else{
            merchantApp.visibility = View.VISIBLE
        }

        if(!isAppInstalled("uk.co.ordervox.merchant2")){
            merchantApp2.visibility = View.GONE
        }
        else{
            merchantApp2.visibility = View.VISIBLE
        }


        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(this@HomeActivity)) {

                preventStatusBarExpansion(this)

            }
        }else{
            preventStatusBarExpansion(this)
        }

        if(intent!=null){

            val value=intent.getStringExtra("app_package")


            if(value!=null){
                if(value=="open_setting"){

                    startActivity(Intent(Settings.ACTION_SETTINGS))



                }else{
                    openApp(value)
                }


            }


        }else{
             Log.e("MESSAGE________", "else")
        }



        // Applying the date / time format (changeable in settings)

        val upperFMT = resources.getStringArray(R.array.settings_launcher_time_formats_upper)
        val lowerFMT = resources.getStringArray(R.array.settings_launcher_time_formats_lower)




    }
  fun openMerchantApp(view: View){

      isMerchant = true
      val packageName = "uk.co.ordervox.merchant"

      var intent = packageManager.getLaunchIntentForPackage(packageName)


      startActivity(intent)

  }



    override fun onPause() {
        isPaused = true

        Log.e("locker____", "INSIDE ON PAUSE");
        super.onPause()
        clockTimer.cancel()
        val activityManager = applicationContext
            .getSystemService(ACTIVITY_SERVICE) as ActivityManager

       Log.e("locker____", "isMerchant : $isMerchant")
        if(!isMerchant){
            Log.e("locker____", "inside if")
          activityManager.moveTaskToFront(taskId, 0)
        }else{
            isMerchant = false;
        }

    }


    override fun onBackPressed() {
        return
    }



    // Tooltip
    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        when(settingsIconShown) {
            true -> {
                hideSettingsIcon()
            }
            false -> showSettingsIcon()
        }
        return false
    }

    private fun showSettingsIcon(){

        settingsIconShown = true

        tooltipTimer = fixedRateTimer("tooltipTimer", true, 10000, 1000) {
            this@HomeActivity.runOnUiThread { hideSettingsIcon() }
        }
    }

    private fun hideSettingsIcon(){
        tooltipTimer.cancel()

        settingsIconShown = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        // Buffer / Debounce the pointer count
        if (event.pointerCount > bufferedPointerCount) {
            bufferedPointerCount = event.pointerCount
            pointerBufferTimer = fixedRateTimer("pointerBufferTimer", true, 300, 1000) {
                bufferedPointerCount = 1
                this.cancel() // a non-recurring timer
            }
        }

        return if (mDetector.onTouchEvent(event)) { false } else { super.onTouchEvent(event) }
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {
       return true;
    }

    fun isAppInstalled(packageName:String):Boolean{
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    }


    override fun onDoubleTapEvent(event: MotionEvent): Boolean { return false }
    override fun onDown(event: MotionEvent): Boolean { return false }
    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {

        return false;

    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, dX: Float, dY: Float): Boolean { return false }
    override fun onLongPress(p0: MotionEvent?) {

    }

    override fun onShowPress(event: MotionEvent) {}
    override fun onSingleTapUp(event: MotionEvent): Boolean { return false }

    fun  openSetting(view: View){
        startActivity(Intent(this,SettingActivity::class.java))

    }



    fun openWalletApp(view: View) {

        isMerchant = true
        val packageName = "uk.co.ordere.merchant_wallet"

        var intent = packageManager.getLaunchIntentForPackage(packageName)

        startActivity(intent)

    }
    fun openMerchantApp2(view: View) {

        isMerchant = true
        val packageName = "uk.co.ordervox.merchant2"

        var intent = packageManager.getLaunchIntentForPackage(packageName)

        startActivity(intent)

    }

    private fun openApp(packageName: String) {




        isMerchant = true


        var intent = packageManager.getLaunchIntentForPackage(packageName)


        startActivity(intent)

    }

    fun checkPermission(){


        if (Build.VERSION.SDK_INT < 23) {
            return
        }
        val myInfo: PackageInfo
        myInfo = try {
            packageManager.getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
        } catch (ex: java.lang.Exception) {
            return
        }
        //check if all required permissions are granted
        //check if all required permissions are granted
        val deniedPermissions = ArrayList<String>()
        for (perm in myInfo.requestedPermissions) {
            if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                _grantedPermissions.add(perm)
            } else {
                deniedPermissions.add(perm)
            }
        }
        //req!
        //req!
        requestPermissions(deniedPermissions.toTypedArray(), _permRequestCode)

    }







}
