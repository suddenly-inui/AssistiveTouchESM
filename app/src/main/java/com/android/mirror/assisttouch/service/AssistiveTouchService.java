package com.android.mirror.assisttouch.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.utils.SystemsUtils;


import java.io.IOException;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AssistiveTouchService extends Service {

    private boolean isMoving;

    private float rawX;
    private float rawY;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mStatusBarHeight;

    private int lastAssistiveTouchViewX;
    private int lastAssistiveTouchViewY;

    private View mAssistiveTouchView;
    private View mInflateAssistiveTouchView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams  mParams;
    private PopupWindow mPopupWindow;
    private AlertDialog.Builder mBulider;
    private AlertDialog mAlertDialog;

    private Timer mTimer;
    private Handler mHandler;

    private LayoutInflater mInflater;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        calculateForMyPhone();
        createAssistiveTouchView();
        inflateViewListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init(){
        mTimer = new Timer();
        mHandler =  new MyHandler();
        mBulider = new AlertDialog.Builder(AssistiveTouchService.this);
        mAlertDialog = mBulider.create();
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mInflater = LayoutInflater.from(this);
        mAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_layout, null);
        mInflateAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_inflate_layout, null);
    }

    private void calculateForMyPhone(){
        DisplayMetrics displayMetrics = SystemsUtils.getScreenSize(this);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this);

        mInflateAssistiveTouchView.setLayoutParams(new WindowManager.LayoutParams((int) (mScreenWidth * 0.75), (int) (mScreenWidth * 0.75)));
    }

    public void createAssistiveTouchView(){
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.x = mScreenWidth;
        mParams.y = 520;
        mParams.gravity = Gravity.TOP|Gravity.START;  // LEFT
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowManager.addView(mAssistiveTouchView, mParams);
        mAssistiveTouchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                rawX = event.getRawX();
                rawY = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:  //ボタンタッチ時
                        isMoving = false;
                        break;
                    case MotionEvent.ACTION_UP:  //ボタン離した時
                        setAssitiveTouchViewAlign();  // 角に戻る処理
                        break;
                    case MotionEvent.ACTION_MOVE:  //ドラッグ時
                        isMoving = true;
                        mParams.x = (int) (rawX - mAssistiveTouchView.getMeasuredWidth() / 2);
                        mParams.y = (int) (rawY - mAssistiveTouchView.getMeasuredHeight() / 2 - mStatusBarHeight);
                        mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
                }
                return isMoving;
            }  // ATに触れた時の処理
        });
        mAssistiveTouchView.setOnClickListener(v -> {  //AT押下時の処理
            mAssistiveTouchView.setAlpha(0);
            lastAssistiveTouchViewX = mParams.x;
            lastAssistiveTouchViewY = mParams.y;
            myAssistiveTouchAnimator(mParams.x,
                    mScreenWidth / 2 - (int)(mScreenWidth*0.75/2),
                    mParams.y,
                    mScreenHeight / 2 - (int)(mScreenWidth*0.75/2),
                    false).start();

            mParams.width = (int) (mScreenWidth*0.75);
            mParams.height = (int) (mScreenWidth*0.75);
            mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);

            mPopupWindow = new PopupWindow(mInflateAssistiveTouchView);

            mPopupWindow.setWidth((int) (mScreenWidth * 0.75));  // 横
            mPopupWindow.setHeight((int) (mScreenWidth * 0.75));  //縦

            mPopupWindow.setOnDismissListener(() -> {
                mParams.width = (int) (WindowManager.LayoutParams.WRAP_CONTENT);
                mParams.height = (int) (WindowManager.LayoutParams.WRAP_CONTENT);
                myAssistiveTouchAnimator(mParams.x, lastAssistiveTouchViewX, mParams.y, lastAssistiveTouchViewY, true).start();
                mAssistiveTouchView.setAlpha(1);
            });  //  ポップアップが閉じる処理
            mPopupWindow.setFocusable(true);
            mPopupWindow.setTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.showAtLocation(mAssistiveTouchView, Gravity.CENTER, 0, 0);  //TODO: ここ治す
        });  //AT押下時の処理
    }

    public void HTTPPost(String u, String json) throws IOException {
        okhttp3.MediaType mediaTypeJson = okhttp3.MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaTypeJson, json);

        final Request request = new Request.Builder()
                .url(u)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback(){
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String resString = response.body().string();
                //view更新のときは handler#post()する
            }

            @Override
            public void onFailure(Call call, IOException arg1) {
                System.out.println(arg1);
            }
        });
    }

    private void inflateViewListener(){
        ImageView shutdown = (ImageView)mInflateAssistiveTouchView.findViewById(R.id.shutdown);
        shutdown.setOnClickListener(v -> {
                    //電源オフ」
                    SystemsUtils.shutDown(AssistiveTouchService.this);

                    //ATを格納
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(0);
                        }
                    }, 600);

                    //HTTP　POST
                    String url = "http://133.27.186.95";
                    final String json =
                        "{\"user\":{" +
                            "\"name\":\"name1\","+
                            "\"password\":\"password\","+
                            "\"password_confirmation\":\"password\""+
                            "}}";
                    try {
                        HTTPPost(url, json);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e);
                    }
                }
        );
    }  // ポップアップ内の機能押下時の処理

    private ValueAnimator myAssistiveTouchAnimator(final int fromx, final int tox, int fromy, final int toy, final boolean flag){
        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("X", fromx, tox);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofInt("Y", fromy, toy);
        ValueAnimator v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2);
        v1.setDuration(100L);
        v1.setInterpolator(new DecelerateInterpolator());
        v1.addUpdateListener(animation -> {
            Integer x = (Integer) animation.getAnimatedValue("X");
            Integer y = (Integer) animation.getAnimatedValue("Y");
            mParams.x = x;
            mParams.y = y;
            mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
        });
        v1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (flag)
                    mAssistiveTouchView.setAlpha(0.85f);
            }
        });
        return v1;
    }

    private void setAssitiveTouchViewAlign(){
        int mAssistiveTouchViewWidth = mAssistiveTouchView.getMeasuredWidth();
        int mAssistiveTouchViewHeight = mAssistiveTouchView.getMeasuredHeight();
        int top = mParams.y + mAssistiveTouchViewWidth/2;
        int left = mParams.x + mAssistiveTouchViewHeight/2;
        int right = mScreenWidth - mParams.x - mAssistiveTouchViewWidth/2;
        int bottom = mScreenHeight - mParams.y - mAssistiveTouchViewHeight/2;
        int lor = Math.min(left, right);
        int tob = Math.min(top, bottom);
        int min = Math.min(lor, tob);
        lastAssistiveTouchViewX = mParams.x;
        lastAssistiveTouchViewY = mParams.y;
        if(min == top) mParams.y = 0;
        if(min == left) mParams.x = 0;
        if(min == right) mParams.x = mScreenWidth - mAssistiveTouchViewWidth;
        if(min == bottom) mParams.y = mScreenHeight - mAssistiveTouchViewHeight;
        myAssistiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start();
    } // ボタン離脱時角に戻る処理

//    private void showScreenshot(String name){
//        String path = "/sdcard/Pictures/" + name + ".png";
//        Bitmap bitmap = BitmapFactory.decodeFile(path);
//
//        mScreenShotView = mInflater.inflate(R.layout.screen_shot_show, null);
//        ImageView imageView = (ImageView)mScreenShotView.findViewById(R.id.screenshot);
//        imageView.setImageBitmap(bitmap);
//
//        mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//        mAlertDialog.show();
//        WindowManager.LayoutParams alertDialogParams = mAlertDialog.getWindow().getAttributes();
//        alertDialogParams.width = mScreenWidth;
//        alertDialogParams.height = mScreenHeight;
//        mAlertDialog.getWindow().setAttributes(alertDialogParams);
//        mAlertDialog.getWindow().setContentView(mScreenShotView);
//
//        mTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                Message msg = mHandler.obtainMessage();
//                msg.what = 2;
//                mHandler.sendMessage(msg);
//            }
//        }, 3000);

        //*mSceenShotAnimator().start();

        //*ObjectAnimator.ofFloat(mScreenShotView, "translationX", 0, mScreenWidth-mScreenShotView.getX());
//        ObjectAnimator.ofFloat(mScreenShotView, "translationY", 0, mScreenHeight-mScreenShotView.getY());
//        ObjectAnimator.ofFloat(mScreenShotView, "scaleX", 1, 0);
//        ObjectAnimator.ofFloat(mScreenShotView, "scaleY", 1, 0);

        //mScreenShotView.setPivotX();
        //mScreenShotView.setPivotY();
        //PropertyValuesHolder p1 = PropertyValuesHolder.ofFloat("X", 0, mScreenWidth);
//        PropertyValuesHolder p2 = PropertyValuesHolder.ofFloat("Y", 0, mScreenHeight/2);
//        PropertyValuesHolder p3 = PropertyValuesHolder.ofFloat("scaleX", 1, 0.5F);
//        PropertyValuesHolder p4 = PropertyValuesHolder.ofFloat("scaleY", 1, 0.5F);
//        ObjectAnimator.ofPropertyValuesHolder(mScreenShotView,p1,p2,p3,p4).setDuration(2000).start();*/
//    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            mPopupWindow.dismiss();
            super.handleMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWindowManager.removeView(mAssistiveTouchView);
    }
}
