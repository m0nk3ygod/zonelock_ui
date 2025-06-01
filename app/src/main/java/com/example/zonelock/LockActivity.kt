package com.example.zonelock

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    private var secretTapCount = 0
    private val resetDelay: Long = 3000
    private val correctPassword = "1234"
    private val handler = Handler(Looper.getMainLooper())

    private val resetTapRunnable = Runnable {
        secretTapCount = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        // 키오스크 모드 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
        }

        hideSystemUI()

//        // 네트워크 새로고침
//        findViewById<Button>(R.id.btn_refresh_network).setOnClickListener {
//            val intent = Intent(this, NetworkMonitorService::class.java)
//            startService(intent)
//            Toast.makeText(this, "네트워크 새로고침 실행", Toast.LENGTH_SHORT).show()
//        }

        // 지도 보기 (보기 전용 모드)
//        findViewById<Button>(R.id.btn_open_map).setOnClickListener {
//            val intent = Intent(this, MapActivity::class.java)
//            intent.putExtra("view_only", true)
//            startActivity(intent)
//        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val x = event.x
            val y = event.y

            if (x > screenWidth * 3 / 4 && y < screenHeight / 4) {
                secretTapCount++
                handler.removeCallbacks(resetTapRunnable)
                handler.postDelayed(resetTapRunnable, resetDelay)

                if (secretTapCount >= 5) {
                    secretTapCount = 0
                    showPasswordDialog()
                }
            } else {
                secretTapCount = 0
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showPasswordDialog() {
        val editText = EditText(this)
        editText.hint = "비밀번호 입력"

        AlertDialog.Builder(this)
            .setTitle("관리자 비밀번호")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                if (editText.text.toString() == correctPassword) {
                    // 🔓 키오스크 모드 해제
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        stopLockTask()
                    }

                    val intent = Intent(this@LockActivity, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onBackPressed() {
        // 뒤로가기 버튼 무시
    }

    override fun onUserLeaveHint() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
            }
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        actionBar?.hide()
    }
}
