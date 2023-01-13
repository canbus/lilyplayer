package com.bao.lilyplayer;



import java.io.File;
import java.io.FileInputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Files;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.provider.Settings;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.media.session.MediaButtonReceiver;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import javax.xml.xpath.XPathExpression;


public class MainActivity extends Activity {

    private static final String TAG = "LilyPlayer";
    private int TIMEOVER = 100;

    private HeadSetReceiver mReceiver = null;
    private MediaPlayer mPlayer = null;
    String curPlayFile = "";
    CheckBox cb1,cb2,cbRadon;
    TextView tv;
    TextView tv2;
    SeekBar seekBar;
    Timer timer;
    TimerTask timerTask ;
    ImageButton btn,btnNext,btnBack,btnVolUp,btnVolDown;
    AudioManager audioManager = null;
    ArrayList<String> fileLists = new ArrayList<String>();
    private MediaSessionCompat mMediaSession = null;

    BroadcastReceiver mBtReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        XXPermissions.with(this)
//                .permission(Permission.READ_EXTERNAL_STORAGE)
//                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .permission(Permission.BLUETOOTH_CONNECT)
                .permission(Permission.BLUETOOTH_ADVERTISE)
                .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                //Toast.makeText(MainActivity.this, "权限获取成功", Toast.LENGTH_SHORT).show();
                            }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        Toast.makeText(MainActivity.this, "权限获取失败", Toast.LENGTH_SHORT).show();
                    }
                });

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(new NextAudio());
        audioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);

        btn = (ImageButton)findViewById(R.id.imageButton1);
        btnNext = (ImageButton)findViewById(R.id.imageButtonNext);
        btnBack = (ImageButton)findViewById(R.id.imageButtonBack);
        btnVolDown = (ImageButton)findViewById(R.id.imageVolDown);
        btnVolUp = (ImageButton)findViewById(R.id.imageVolUp);

        tv = (TextView) findViewById(R.id.textView1);
        tv2 = (TextView) findViewById(R.id.textView2);
        cb1 = (CheckBox) findViewById(R.id.checkBox1);
        cb2 = (CheckBox) findViewById(R.id.checkBox2);
        cb2.setChecked(true);
        cbRadon = findViewById(R.id.checkBox3);

        cbRadon.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                getFileList();
            }
        });
        cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    cb2.setChecked(false);
            }
        });
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mPlayer != null)
                {
                    if(mPlayer.isPlaying())
                    {
                        mPlayer.pause();
                        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_play);
                        btn.setImageBitmap(drawable.getBitmap());
                    }

                    else {
                        mPlayer.start();
                        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_pause);
                        btn.setImageBitmap(drawable.getBitmap());
                    }
                }

            }
        });
        btnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        });
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                playPre();
            }
        });
        btnVolDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_PLAY_SOUND);
            }
        });
        btnVolUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);
                //audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_PLAY_SOUND);
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    mPlayer.seekTo(progress);
                }

            }
        });

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if(uri == null){
            Toast.makeText(this, "请用浏览器打开音频文件", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, uri.toString());
        try {
            String fileName = "";// = Uri.decode(uri.toString()).substring(7);
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {"_data"};
                Cursor cursor = null;
                try {
                    cursor = this.getContentResolver().query(uri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                fileName = uri.getPath();
            }


            //verifyStoragePermissions(this);

    //		 String fileName = "/mnt/sdcard/1.mp3";

            if (fileName.length() > 3) {

                curPlayFile = fileName;

                play(curPlayFile);

                Timer timer = new Timer();
                timerTask = new Task();
                timer.schedule(timerTask, 1000, 1000);

                cb2.setOnCheckedChangeListener(new IndexCycle());
                getFileList();
            }
        }catch(Exception e) {}

        mBtReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive: "+action);
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,0);
                if(action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) && state == BluetoothAdapter.STATE_DISCONNECTED){
                    if(mPlayer != null) {
                        mPlayer.pause();
                        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_play);
                        btn.setImageBitmap(drawable.getBitmap());
                    }
                }
            }
        };
        mReceiver = new HeadSetReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        Log.d(TAG, "registerReceiver  ACTION_CONNECTION_STATE_CHANGED");

//        AudioManager audiomanage = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        PackageManager packageManager = getPackageManager();
//        //packageManager.setComponentEnabledSetting(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,packageManager.DONT_KILL_APP);
//        MediaSessionCompat mediaSessionCompat = MediaSessionCompat.fromMediaSession(this,"mbr");


        registerReceiver(mBtReceiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)); //蓝牙耳机连接状态

        ComponentName mbr = new ComponentName(getPackageName(),MediaButtonReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(this,"mbr",mbr,null);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        if(!mMediaSession.isActive()){
            mMediaSession.setActive(true);
        }
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                    String action = intent.getAction();
                    if (action != null) {
                        if (TextUtils.equals(action, Intent.ACTION_MEDIA_BUTTON)) {
                            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                            if (keyEvent != null) {
                                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                    int keyCode = keyEvent.getKeyCode();
                                    switch (keyCode) {
                                        //可以通过广播形式通知Activity更新UI
                                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                                            //Toast.makeText(MainActivity.this, "onReceive: 播放", Toast.LENGTH_SHORT).show();
                                            break;
                                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                            if(mPlayer != null)
                                                mPlayer.pause();
                                            //Toast.makeText(MainActivity.this, "onReceive: 暂停", Toast.LENGTH_SHORT).show();
                                            break;
                                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                                            playNext();
                                            //Toast.makeText(MainActivity.this, "onReceive: 下一首", Toast.LENGTH_SHORT).show();
                                            break;
                                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                            playPre();
                                            //Toast.makeText(MainActivity.this, "onReceive: 上一首", Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            }
                        }
                    }
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { //音量键24,25
        //Log.d(TAG, "onKeyDown: "+keyCode);
        //Toast.makeText(this, ""+keyCode, Toast.LENGTH_SHORT).show();
        return super.onKeyDown(keyCode, event);
    }



    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
//        int requestCode = 200;
//        requestPermissions(permissions, requestCode);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // > 31
                   && ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions error ");
                List<String> requestList = new ArrayList<String>();
                requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
                ActivityCompat.requestPermissions(activity,requestList.toArray(new String[0]), 1);
            }
        }
    }
//    public void verifyStoragePermissions(Activity activity) {
//
//        try {
//            //检测是否有写的权限
//            int permission = ActivityCompat.checkSelfPermission(activity,"android.permission.READ_EXTERNAL_STORAGE");
//            if (permission != PackageManager.PERMISSION_GRANTED) {
//                // 没有写的权限，去申请写的权限，会弹出对话框
//                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
//            }
//            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // > 31
//                   && ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "Permissions error ");
//                List<String> requestList = new ArrayList<String>();
//                requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
//                requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
//                ActivityCompat.requestPermissions(activity,requestList.toArray(new String[0]), 1);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    //在onCreate()方法中调用该方法即可

    private void getFileList() {
        File file = new File(curPlayFile);

        file = new File(file.getParent());
        if (file.isDirectory()) {
            fileLists.clear();
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                file = files[i];
                if (file.isFile()) {
                    String filename = file.getName();
                    String type = filename.substring(filename.lastIndexOf(".") + 1);
                    type = type.toLowerCase();
                    if (type.contains("mp3") || type.contains("wav") || type.contains("m4a") || type.contains("ape")) {
                        fileLists.add(file.getPath());
                    }
                }
            }
            if(cbRadon.isChecked())
                Collections.shuffle(fileLists);
            else
                Collections.sort(fileLists);
        }
    }

    class IndexCycle implements OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            if (isChecked) {
                cb1.setChecked(false);
                getFileList();
            }
        }

    }

    class Task extends TimerTask {

        @Override
        public void run() {
            if (mPlayer != null) {
                {
//                    Message msg = new Message();
//                    msg.what = TIMEOVER ;
//                    handler.sendMessage(msg );
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if(seekBar != null ) seekBar.setProgress(mPlayer.getCurrentPosition());
                                int duration = mPlayer.getDuration() / 1000;
                                int curPos = mPlayer.getCurrentPosition() / 1000;
                                if(tv2 != null) tv2.setText(( curPos > 60 ? ( curPos/60+":"+curPos%60):(curPos)) + "/" + (duration > 60 ? (duration / 60 +":"+ duration %60):(duration)));

                            }catch (Exception e){}
                            }
                    });
                }
            }
        }

    }

    class NextAudio implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {

            if (cb1.isChecked()) {
                //play(curPlayFile);
                playCur();
            }
            if (cb2.isChecked()) {
                playNext();
            } else {
                mp.stop();
            }
        }

    }
    void playCur()
    {
        playNext();
        playPre();
    }
    void playNext()
    {
        if(mPlayer == null)
            return;
        if (fileLists.size() > 2) {
            //Log.d(TAG, curPlayFile);
            for (int i = 0; i < fileLists.size() - 1; i++) {
                String tmp = fileLists.get(i);
                //Log.d(TAG, i+":"+tmp);
                if (tmp.contains(curPlayFile)) {
                    curPlayFile = fileLists.get(i + 1);
                    play(curPlayFile);
                    //Log.d(TAG, "play "+ curPlayFile);
                    break;
                }
            }
        }
    }
    void playPre()
    {
        if(mPlayer == null)
            return;
        if (fileLists.size() > 2) {
            //Log.d(TAG, curPlayFile);
            for (int i = 0; i < fileLists.size() ; i++) {
                String tmp = fileLists.get(i);
                //Log.d(TAG, i+":"+tmp);
                if (tmp.contains(curPlayFile)) {
                    if(i > 0)
                        curPlayFile = fileLists.get(i - 1);
                    play(curPlayFile);
                    //Log.d(TAG, "play "+ curPlayFile);
                    break;
                }
            }
        }
    }
    void play(String file) {
        tv.setText(new File(file).getName());
        try {
//            File tempFile = new File(file);
//            FileInputStream fis = new FileInputStream(tempFile);

            mPlayer.reset();
            //mPlayer.setDataSource(Uri.parse(videoUrl));
            //mPlayer.setDataSource(fis.getFD());//
            mPlayer.setDataSource(file);
            mPlayer.prepare();
            mPlayer.start();
            seekBar.setMax(mPlayer.getDuration());
            BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_pause);
            btn.setImageBitmap(drawable.getBitmap());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "play: "+e.toString());
        }

    }

    @Override
    protected void onStop() {
//		if(mPlayer != null)
//			mPlayer.stop();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if(mBtReceiver != null)
            unregisterReceiver(mBtReceiver);
        if(mReceiver != null)
            unregisterReceiver(mReceiver);
        if(timer != null)
        {
            timer.cancel();
            timer = null;
        }
        if(timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
        if (mPlayer != null) {
            mPlayer.pause();
            mPlayer.release();
            mPlayer = null;
        }
        super.onDestroy();

    }

    class HeadSetReceiver extends BroadcastReceiver {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d(TAG, "onReceive: "+action);
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                Log.d(TAG, "onReceive: "+adapter.getProfileConnectionState(BluetoothProfile.HEADSET));
                if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                    //Log.d(TAG, "onReceive: STATE_DISCONNECTED");
                    if(mPlayer != null){
                        mPlayer.pause();
                        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_play);
                        btn.setImageBitmap(drawable.getBitmap());
                    }
                }else if (BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                    //Log.d(TAG, "onReceive: STATE_CONNECTED");
                    if(mPlayer != null) {
                        mPlayer.start();
                        BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.player_pause);
                        btn.setImageBitmap(drawable.getBitmap());
                    }
                }

            } else if ("android.intent.action.HEADSET_PLUG".equals(action)) { //耳机插入
                if (intent.hasExtra("state")) {
//                    if (intent.getIntExtra("state", 0) == 0) {
//                        Toast.makeText(context, "headset not connected", Toast.LENGTH_LONG).show();
//                    } else if (intent.getIntExtra("state", 0) == 1) {
//                        Toast.makeText(context, "headset connected", Toast.LENGTH_LONG).show();
//                    }
                }
            }
        }
    }
}
