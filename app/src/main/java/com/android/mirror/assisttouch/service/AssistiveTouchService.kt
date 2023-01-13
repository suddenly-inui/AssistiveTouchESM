package com.android.mirror.assisttouch.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.PopupWindow
import com.android.mirror.assisttouch.R
import com.android.mirror.assisttouch.utils.SystemsUtils
import com.google.gson.Gson
import okhttp3.*
import java.time.format.DateTimeFormatter
import java.util.*


class AssistiveTouchService : Service() {
    private var isMoving = false
    private var rawX = 0f
    private var rawY = 0f
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mStatusBarHeight = 0
    private var lastAssistiveTouchViewX = 0
    private var lastAssistiveTouchViewY = 0
    private var mAssistiveTouchView: View? = null
    private var mInflateAssistiveTouchView: View? = null
    private var mWindowManager: WindowManager? = null
    private var mParams: WindowManager.LayoutParams? = null
    private var mPopupWindow: PopupWindow? = null
    private var mBulider: AlertDialog.Builder? = null
    private var mAlertDialog: AlertDialog? = null
    private var mTimer: Timer? = null
    private var mHandler: Handler? = null
    private var mInflater: LayoutInflater? = null
    private var jd: JsonData = JsonData.getInstance()
    private val gson:Gson = Gson()
    private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("ss.S")
    private var t = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        init()
        calculateForMyPhone()
        createAssistiveTouchView()
        inflateViewListener()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun init() {
        mTimer = Timer()
        mHandler = MyHandler()
        mBulider = AlertDialog.Builder(this@AssistiveTouchService)
        mAlertDialog = mBulider!!.create()
        mParams = WindowManager.LayoutParams()
        mWindowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        mInflater = LayoutInflater.from(this@AssistiveTouchService)
        mAssistiveTouchView = mInflater?.inflate(R.layout.assistive_touch_layout, null)
        mInflateAssistiveTouchView =
            mInflater?.inflate(R.layout.assistive_touch_inflate_layout, null)
    }

    private fun calculateForMyPhone() {
        val displayMetrics = SystemsUtils.getScreenSize(this)
        mScreenWidth = displayMetrics.widthPixels
        mScreenHeight = displayMetrics.heightPixels
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this)
        mInflateAssistiveTouchView!!.layoutParams =
            WindowManager.LayoutParams((mScreenWidth * 0.75).toInt(), (mScreenWidth * 0.75).toInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createAssistiveTouchView() {
        mParams!!.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        if (Build.VERSION.SDK_INT > 25){
            mParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        mParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        mParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        mParams!!.x = mScreenWidth
        mParams!!.y = 520
        mParams!!.gravity = Gravity.TOP or Gravity.START
        mParams!!.format = PixelFormat.RGBA_8888
        mParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWindowManager!!.addView(mAssistiveTouchView, mParams)

        fun inflateATview(){
            mAssistiveTouchView!!.alpha = 0f
            if(mParams!!.x > mScreenWidth/2){
                lastAssistiveTouchViewX = mScreenWidth
            }else{
                lastAssistiveTouchViewX = 0
            }
            lastAssistiveTouchViewY = mParams!!.y
//            myAssistiveTouchAnimator(
//                mParams!!.x,
//                mScreenWidth / 2 - (mScreenWidth * 0.75 / 2).toInt(),
//                mParams!!.y,
//                mScreenHeight / 2 - (mScreenWidth * 0.75 / 2).toInt(),
//                false
//            ).start()

            if(Build.VERSION.SDK_INT < 26){
                mParams!!.width = mScreenWidth
                mParams!!.height = (mScreenWidth * 0.75).toInt()
            }
            mParams!!.y = mScreenHeight/2//
            mWindowManager!!.updateViewLayout(mAssistiveTouchView, mParams)

            mPopupWindow = PopupWindow(mInflateAssistiveTouchView)
            mPopupWindow!!.width = mScreenWidth // 横
            mPopupWindow!!.height = (mScreenWidth * 0.75).toInt() //縦
            //  ポップアップが閉じる処理
            mPopupWindow!!.setOnDismissListener {
                mPopupWindow!!.dismiss()
                mParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
                mParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
                myAssistiveTouchAnimator(
                    mParams!!.x,
                    lastAssistiveTouchViewX,
                    mParams!!.y,
                    lastAssistiveTouchViewY,
                    true
                ).start()
                mAssistiveTouchView!!.alpha = .85f
            }

            mPopupWindow!!.isFocusable = true
            mPopupWindow!!.isTouchable = true
            //mPopupWindow!!.setBackgroundDrawable(BitmapDrawable())
            mPopupWindow!!.showAtLocation(mAssistiveTouchView, Gravity.CENTER, 0, 0)
        }

        mAssistiveTouchView!!.setOnTouchListener { _, event ->
            rawX = event.rawX
            rawY = event.rawY

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMoving = false
                    t = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    if(System.currentTimeMillis() - t < 100){
                        mAssistiveTouchView!!.alpha = 0f
                        inflateATview()
                    }else{
                        setAssitiveTouchViewAlign()
                    }
                } // 角に戻る処理
                MotionEvent.ACTION_MOVE -> {
                    isMoving = true
                    mParams!!.x = (rawX - mAssistiveTouchView!!.measuredWidth / 2).toInt()
                    mParams!!.y =
                        (rawY - mAssistiveTouchView!!.measuredHeight / 2 - mStatusBarHeight).toInt()
                    mWindowManager!!.updateViewLayout(mAssistiveTouchView, mParams)
                }
            }
            isMoving
        } // ATに触れた時の処理

        mAssistiveTouchView!!.setOnClickListener {     //AT押下時の処理
            //inflateATview()
        } //AT押下時の処理


    }

    private fun inflateViewListener() {
        val shutdown1 = mInflateAssistiveTouchView!!.findViewById<View>(R.id.shutdown1) as ImageView
        val shutdown2 = mInflateAssistiveTouchView!!.findViewById<View>(R.id.shutdown2) as ImageView
        val shutdown3 = mInflateAssistiveTouchView!!.findViewById<View>(R.id.shutdown3) as ImageView

        fun shutdown(l:String){
            //電源オフ」
            SystemsUtils.shutDown(this@AssistiveTouchService)

            //label更新
            jd.label = l

            //データ保存
            //jd.saveJson()
            jd.saveAcc(output = true)
            jd.saveGyro(output = true)

            //ATを格納
            mTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    mHandler!!.sendEmptyMessage(0)
                }
            }, 600)
        }
        shutdown1.setOnClickListener {shutdown("1")}
        shutdown2.setOnClickListener {shutdown("2")}
        shutdown3.setOnClickListener {shutdown("3")}
    } // ポップアップ内の機能押下時の処理

    private fun myAssistiveTouchAnimator(
        fromx: Int,
        tox: Int,
        fromy: Int,
        toy: Int,
        flag: Boolean
    ): ValueAnimator {
        val p1 = PropertyValuesHolder.ofInt("X", fromx, tox)
        val p2 = PropertyValuesHolder.ofInt("Y", fromy, toy)
        val v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2)
        v1.duration = 100L
        v1.interpolator = DecelerateInterpolator()
        v1.addUpdateListener { animation: ValueAnimator ->
            val x = animation.getAnimatedValue("X") as Int
            val y = animation.getAnimatedValue("Y") as Int
            mParams!!.x = x
            mParams!!.y = y
            mWindowManager!!.updateViewLayout(mAssistiveTouchView, mParams)
        }
        v1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (flag) mAssistiveTouchView!!.alpha = 0.85f
            }
        })
        return v1
    }

    private fun setAssitiveTouchViewAlign() {
        val mAssistiveTouchViewWidth = mAssistiveTouchView!!.measuredWidth
        val mAssistiveTouchViewHeight = mAssistiveTouchView!!.measuredHeight
        val top = mParams!!.y + mAssistiveTouchViewWidth / 2
        val left = mParams!!.x + mAssistiveTouchViewHeight / 2
        val right = mScreenWidth - mParams!!.x - mAssistiveTouchViewWidth / 2
        val bottom = mScreenHeight - mParams!!.y - mAssistiveTouchViewHeight / 2
        val lor = Math.min(left, right)
        val tob = Math.min(top, bottom)
        val min = Math.min(lor, tob)
        lastAssistiveTouchViewX = mParams!!.x
        lastAssistiveTouchViewY = mParams!!.y
        if (min == top) mParams!!.y = 0
        if (min == left) mParams!!.x = 0
        if (min == right) mParams!!.x = mScreenWidth - mAssistiveTouchViewWidth
        if (min == bottom) mParams!!.y = mScreenHeight - mAssistiveTouchViewHeight
        myAssistiveTouchAnimator(
            lastAssistiveTouchViewX,
            mParams!!.x,
            lastAssistiveTouchViewY,
            mParams!!.y,
            false
        ).start()
    } // ボタン離脱時角に戻る処理

    private inner class MyHandler : Handler() {
        override fun handleMessage(msg: Message) {
            mPopupWindow!!.dismiss()
            super.handleMessage(msg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mWindowManager!!.removeView(mAssistiveTouchView)
    }
}