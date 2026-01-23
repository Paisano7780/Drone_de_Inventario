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
import com.paisano.droneinventoryscanner.databinding.OverlayStatusBinding

/**
 * OverlayService - Manages floating overlay UI for scan feedback
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_SHOW_IDLE = "show_idle"
        const val ACTION_SHOW_SUCCESS = "show_success"
        const val ACTION_SHOW_DUPLICATE_DECISION = "show_duplicate_decision"
        const val ACTION_HIDE_ALL = "hide_all"
        const val EXTRA_CODE = "code"
        
        var duplicateDecisionCallback: DuplicateDecisionCallback? = null
        
        fun canDrawOverlays(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || 
                   Settings.canDrawOverlays(context)
        }
        
        fun setIdleState(context: Context) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_IDLE
            }
            context.startService(intent)
        }
        
        fun setSuccessState(context: Context, code: String) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW_SUCCESS
                putExtra(EXTRA_CODE, code)
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
        
        fun hideAllOverlays(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE_ALL
            }
            context.startService(intent)
        }
    }

    interface DuplicateDecisionCallback {
        fun onLoadAnyway(code: String)
        fun onDiscard(code: String)
    }

    private var windowManager: WindowManager? = null
    private var statusOverlayView: View? = null
    private var statusBinding: OverlayStatusBinding? = null
    private var duplicateOverlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var revertToIdleRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_IDLE -> showIdleState()
            ACTION_SHOW_SUCCESS -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: ""
                showSuccessState(code)
            }
            ACTION_SHOW_DUPLICATE_DECISION -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: ""
                showDuplicateDecisionOverlay(code)
            }
            ACTION_HIDE_ALL -> hideAllOverlays()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showIdleState() {
        handler.post {
            try {
                // Cancel any pending revert to idle
                revertToIdleRunnable?.let { handler.removeCallbacks(it) }
                
                // Create or update the status overlay
                if (statusOverlayView == null) {
                    createStatusOverlay()
                }
                
                // Update to idle state
                statusBinding?.apply {
                    statusCard.setCardBackgroundColor(getColor(R.color.status_idle))
                    tvStatusMessage.text = getString(R.string.status_idle)
                    tvStatusMessage.setTextColor(getColor(R.color.black))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing idle state", e)
            }
        }
    }

    private fun showSuccessState(code: String) {
        handler.post {
            try {
                // Cancel any pending revert to idle
                revertToIdleRunnable?.let { handler.removeCallbacks(it) }
                
                // Create or update the status overlay
                if (statusOverlayView == null) {
                    createStatusOverlay()
                }
                
                // Update to success state
                statusBinding?.apply {
                    statusCard.setCardBackgroundColor(getColor(R.color.status_success))
                    tvStatusMessage.text = getString(R.string.status_success, code)
                    tvStatusMessage.setTextColor(getColor(R.color.white))
                }
                
                // Schedule revert to idle after 2 seconds
                revertToIdleRunnable = Runnable {
                    showIdleState()
                }.also { runnable ->
                    handler.postDelayed(runnable, 2000)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error showing success state", e)
            }
        }
    }

    private fun createStatusOverlay() {
        try {
            statusBinding = OverlayStatusBinding.inflate(LayoutInflater.from(this))
            statusOverlayView = statusBinding?.root

            val params = createOverlayParams(
                width = WindowManager.LayoutParams.WRAP_CONTENT,
                height = WindowManager.LayoutParams.WRAP_CONTENT,
                gravity = Gravity.TOP or Gravity.END,
                focusable = false // Display-only, no interaction needed
            )
            
            params.x = 20
            params.y = 100

            windowManager?.addView(statusOverlayView, params)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating status overlay", e)
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
                    gravity = Gravity.CENTER,
                    focusable = true // Needs to be focusable for button interaction
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
        gravity: Int,
        focusable: Boolean = false
    ): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        // Use appropriate flags based on whether interaction is needed
        val flags = if (focusable) {
            // Focusable for interactive overlays (buttons)
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            // Non-focusable for display-only overlays
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }
        
        return WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            flags,
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

    private fun hideStatusOverlay() {
        statusOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing status overlay", e)
            }
            statusOverlayView = null
            statusBinding = null
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
        hideStatusOverlay()
        hideDuplicateOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        revertToIdleRunnable?.let { handler.removeCallbacks(it) }
        hideAllOverlays()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed - cleaning up overlay")
        revertToIdleRunnable?.let { handler.removeCallbacks(it) }
        hideAllOverlays()
        stopSelf()
    }
}
