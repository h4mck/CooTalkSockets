package com.example.cootalksockets


import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.CallStyle
import android.app.Notification.FLAG_LOCAL_ONLY
import android.app.Notification.Style
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.cootalksockets.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import com.example.cootalksockets.Sip


class MainActivity : ComponentActivity() {


    val client = Client()
    val ACTION_ACCEPT_CALL = "101"
    val ctClient = CT_Client()


    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //var permission_granted = false

        //RetrieveFeedTask().execute(urlToRssFeed)

        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

//        val requestPermissionLauncher =
//            registerForActivityResult(
//                ActivityResultContracts.RequestPermission()
//            ) { isGranted: Boolean ->
//                if (isGranted) {
//                    Toast.makeText(this, "Спасибо за разрешение, теперь приложение может корректно работать!", Toast.LENGTH_SHORT).show()
//                    //permission_granted = true
//                } else {
//                    ActivityResultContracts.RequestPermission()
//                }
//            }


        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(
                        this,
                        "Спасибо за разрешение, теперь приложение может корректно работать!",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    Toast.makeText(
                        this,
                        "К сожалению, без этого разрешения основной функционал приложения не будет работать!\nЧтобы разблокировать функционал, перейдите в настройки\n" +
                                "и предоставьте приложению необходимые разрешения.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        //requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        //runBlocking { client() }

//        Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show()
//
//        ip.forEach{
//            Toast.makeText(this, it.toByteArray().toString(), Toast.LENGTH_SHORT).show()
//        }



        //val client = Socket("10.193.186.194", 12345)

        //binding.button.setOnApplyWindowInsetsListener { v, insets ->  }
        
        binding.button.setOnClickListener {

            var clientName = binding.editTextText.text.toString()

            //client.connect(clientName)



            Toast.makeText(this, "Connecting: ${binding.editTextText.text}", Toast.LENGTH_SHORT)
                .show()


            Executors.newSingleThreadExecutor().execute {

                ctClient.init(clientName)

                if (ctClient.setup(ctClient.SERVERIP, ctClient.PORT)) {
                    if (ctClient.auth()) {
                        //Toast.makeText(this, "Login complete", Toast.LENGTH_SHORT)
                        //    .show()
                    } else {
                        //Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT)
                        //    .show()
                    }
                }



                //other tcp handlers work in this thread
//                Executors.newSingleThreadExecutor().execute {
//
//                }

            }

        }

        binding.buttonCh1.setOnClickListener {

            Executors.newSingleThreadExecutor().execute {
                ctClient.sip.sendConnect("ch1")
            }

        }


        binding.buttonChk.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                client.soundCheck()


            } else {

                val intent = Intent(Settings.ACTION_SETTINGS)
                //val intent = Intent("android.settings.APP_PERMISSIONS_SETTINGS")
                startActivity(intent)
                //Toast.makeText(this, "Данная функция недоступна без разрешения", Toast.LENGTH_SHORT).show()

            }
        }

        binding.buttonChkCallAlert.setOnClickListener {

            Executors.newSingleThreadExecutor().execute() {




//            val ll = findViewById<LinearLayout>(R.id.linearLayout)
//
//            ll.addView()

                val person: Person = Person.Builder()
                    .setName("ANONYMOUS")
                    .setImportant(false)
                    .build()

                val intent = Intent(this, MainActivity::class.java)

                val intentPendingAnswer =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                val intentPendingDecline =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                //val incomingCall = CallStyle.Builder()

                val notifChannel = NotificationChannel("0", "Call", NotificationManager.IMPORTANCE_HIGH)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(notifChannel)

                val callNotif: Notification? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Notification.Builder(this, "0")
                        .setStyle(
                            Notification.CallStyle.forIncomingCall(
                                person,
                                intentPendingDecline,
                                intentPendingAnswer
                            )
                        )
                        .setSmallIcon(R.drawable.ic_menu_call)
                        .build()
                } else {
                    null
                }



                //notificationManager.notify(0, callNotif)





                notificationManager.notify(0, callNotif)



//                val intentTo = Intent(this, CallService::class.java) // Build the intent for the service
//                this.startForegroundService(intent)



            }

        }

        binding.buttonChkMediaAlert.setOnClickListener {

            Executors.newSingleThreadExecutor().execute() {


                //val ACTION_ACCEPT_CALL = "101"
// We create a normal intent, just like when we start a new Activity
                val intent = Intent(applicationContext, MainActivity::class.java).apply { action = ACTION_ACCEPT_CALL}
// But we don’t run it ourselves, we pass it to PendingIntent, which will be called later when the button is pressed
                val acceptCallIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)


                val notificationManager = NotificationManagerCompat.from(this)

                val INCOMING_CALL_CHANNEL_ID = "incoming_call"
// Creating an object with channel data
                val channel = NotificationChannelCompat.Builder(
                    // channel ID, it must be unique within the package
                    INCOMING_CALL_CHANNEL_ID,
                    // The importance of the notification affects whether the notification makes a sound, is shown immediately, and so on. We set it to maximum, it’s a call after all.
                    NotificationManagerCompat.IMPORTANCE_HIGH
                )
                    // the name of the channel, which will be displayed in the system notification settings of the application
                    .setName("Incoming calls")
                // channel description, will be displayed in the same place
                .setDescription("Incoming audio and video call alerts")
                .build()
// Creating the channel. If such a channel already exists, nothing happens, so this method can be used before sending each notification to the channel.
                notificationManager.createNotificationChannel(channel)

                val notificationBuilder = NotificationCompat.Builder(
                    this,
                    // channel ID again
                    INCOMING_CALL_CHANNEL_ID
                )
                    // A small icon that will be displayed in the status bar
                    .setSmallIcon(R.drawable.ic_menu_call)
                    // Notification title
                    .setContentTitle("Incoming call")
                // Notification text, usually the caller’s name
                .setContentText("James Smith")
                // Large image, usually a photo / avatar of the caller
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_menu_call))
                // For notification of an incoming call, it’s wise to make it so that it can’t be “swiped”
                .setOngoing(true)

                val actionAccept = NotificationCompat.Action.Builder(
                    // The icon that will be displayed on the button (or not, depends on the Android version)
                    IconCompat.createWithResource(applicationContext, R.drawable.ic_menu_call),
                    // The text on the button
                    "Accept",
                    // The action itself, PendingIntent
                    acceptCallIntent
                ).build()

                val actionDecline = NotificationCompat.Action.Builder(
                    // The icon that will be displayed on the button (or not, depends on the Android version)
                    IconCompat.createWithResource(applicationContext, R.drawable.ic_menu_call),
                    // The text on the button
                    "Decline",
                    // The action itself, PendingIntent
                    acceptCallIntent
                ).build()

                notificationBuilder.addAction(actionAccept)
                notificationBuilder.addAction(actionDecline)

                //So far we’ve only created a sort of “description” of the notification, but it’s not yet shown to the user. To display it, let’s turn to the manager again:
// Let’s get to building our notification
                val notification = notificationBuilder.build()
// We ask the system to display it
                notificationManager.notify(0, notification)



            }

        }

        binding.buttonChkMediaAlert2.setOnClickListener {

            Executors.newSingleThreadExecutor().execute() {

                val notificationManager = NotificationManagerCompat.from(this)

                val INCOMING_CALL_CHANNEL_ID = "incoming_call"
// Creating an object with channel data
                val channel = NotificationChannelCompat.Builder(
                    // channel ID, it must be unique within the package
                    INCOMING_CALL_CHANNEL_ID,
                    // The importance of the notification affects whether the notification makes a sound, is shown immediately, and so on. We set it to maximum, it’s a call after all.
                    NotificationManagerCompat.IMPORTANCE_HIGH
                )
                    // the name of the channel, which will be displayed in the system notification settings of the application
                    .setName("Incoming calls")
                    // channel description, will be displayed in the same place
                    .setDescription("Incoming audio and video call alerts")
                    .build()
// Creating the channel. If such a channel already exists, nothing happens, so this method can be used before sending each notification to the channel.
                notificationManager.createNotificationChannel(channel)

                val acceptCallIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Creating a notification with Notification.CallStyle
                    val icon = Icon.createWithResource(this, R.drawable.star_on)
                    val caller = Person.Builder()
                        // Caller icon
                        .setIcon(icon)
                        // Caller name
                        .setName("Chuck Norris")
                        .setImportant(true)
                        .build()
                    // Creating the call notification style
                    val notificationStyle = Notification.CallStyle.forIncomingCall(caller, acceptCallIntent, acceptCallIntent)
                    val notification = Notification.Builder(this, "incoming_call")
                        .setSmallIcon(R.drawable.star_on)
                        .setContentTitle("Incoming call")
                        .setContentText("Incoming call from Chuck Norris")
                        .setStyle(notificationStyle)
                        // Intent that will be called for when tapping on the notification
                        .setContentIntent(acceptCallIntent)
                        .setFullScreenIntent(acceptCallIntent, true)
                        .setOngoing(true)
                        // notification category that describes this Notification. May be used by the system for ranking and filtering
                        .setCategory(Notification.CATEGORY_CALL)
                        .build()

                    notificationManager.notify(0, notification)

                }

            }

        }

        binding.buttonTest.setOnClickListener {
            ctClient.sip.createConnectPkg("test")
        }

        }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent!!)
//        if (intent.action == ACTION_ACCEPT_CALL) client.call()
//    }


}






