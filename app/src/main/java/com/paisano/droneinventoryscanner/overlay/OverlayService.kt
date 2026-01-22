package com.paisano.droneinventoryscanner.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.paisano.droneinventoryscanner.R
import com.paisano.droneinventoryscanner.databinding.OverlayDuplicateDecisionBinding
import com.paisano.droneinventoryscanner.databinding.OverlaySuccessBinding

/**
 * OverlayService - Manages floating overlay UI for scan feedback
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_SHOW_SUCCESS = "show_success"
        const val ACTION_SHOW_DUPLICATE_DECISION = "show_duplicate_decision"
        const val ACTION_HIDE_ALL = "hide_all"
        const val EXTRA_CODE = "code"
        
        var duplicateDecisionCallback: DuplicateDecisionCallback? = null
        
        fun canDrawOverlays(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || 
                   Settings.canDrawOverlays(context)
        }
        
        fun showSuccess(context: Context) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_SUCCESS
            }
            context.startService(intent)
        }
        
        fun showDuplicateDecision(context: Context, code: String) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_DUPLICATE_DECISION
                putExtra(EXTRA_CODE, code)
            }
            context.startService(intent)
        }
    }

    interface DuplicateDecisionCallback {
        fun onLoadAnyway(code: String)
        fun onDiscard(code: String)
    }

    private var windowManager: WindowManager? = null
    private var successOverlayView: View? = null
    private var duplicateOverlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_SUCCESS -> showSuccessOverlay()
            ACTION_SHOW_DUPLICATE_DECISION -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: ""
                showDuplicateDecisionOverlay(code)
            }
            ACTION_HIDE_ALL -> hideAllOverlays()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showSuccessOverlay() {
        handler.post {
            try {
                // Remove existing success overlay if present
                hideSuccessOverlay()
                
                val binding = OverlaySuccessBinding.inflate(LayoutInflater.from(this))
                successOverlayView = binding.root

                val params = createOverlayParams(
                    width = WindowManager.LayoutParams.WRAP_CONTENT,
                    height = WindowManager.LayoutParams.WRAP_CONTENT,
                    gravity = Gravity.TOP or Gravity.END
                )
                
                params.x = 20
                params.y = 100

                windowManager?.addView(successOverlayView, params)
                
                // Fade out after 2 seconds
                handler.postDelayed({
                    fadeOutAndRemove(successOverlayView) {
                        hideSuccessOverlay()
                    }
                }, 2000)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing success overlay", e)
            }
        }
    }

    private fun showDuplicateDecisionOverlay(code: String) {
        handler.post {
            try {
                // Remove existing duplicate overlay if present
                hideDuplicateOverlay()
                
                val binding = OverlayDuplicateDecisionBinding.inflate(LayoutInflater.from(this))
                duplicateOverlayView = binding.root
                
                // Set the code text
                binding.tvDuplicateCode.text = getString(R.string.code_repeated, code)
                
                // Set up button listeners
                binding.btnLoadAnyway.setOnClickListener {
                    duplicateDecisionCallback?.onLoadAnyway(code)
                    hideDuplicateOverlay()
                }
                
                binding.btnDiscard.setOnClickListener {
                    duplicateDecisionCallback?.onDiscard(code)
                    hideDuplicateOverlay()
                }

                val params = createOverlayParams(
                    width = WindowManager.LayoutParams.MATCH_PARENT,
                    height = WindowManager.LayoutParams.WRAP_CONTENT,
                    gravity = Gravity.CENTER
                )
                
                params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()

                windowManager?.addView(duplicateOverlayView, params)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing duplicate decision overlay", e)
            }
        }
    }

    private fun createOverlayParams(
        width: Int,
        height: Int,
        gravity: Int
    ): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
        }
    }

    private fun fadeOutAndRemove(view: View?, onComplete: () -> Unit) {
        view?.let {
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 300
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        onComplete()
                    }
                })
            }
            it.startAnimation(fadeOut)
        }
    }

    private fun hideSuccessOverlay() {
        successOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing success overlay", e)
            }
            successOverlayView = null
        }
    }

    private fun hideDuplicateOverlay() {
        duplicateOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing duplicate overlay", e)
            }
            duplicateOverlayView = null
        }
    }

    private fun hideAllOverlays() {
        hideSuccessOverlay()
        hideDuplicateOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideAllOverlays()
    }
}
