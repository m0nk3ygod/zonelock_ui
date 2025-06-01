package com.example.zonelock

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private val polygonCoords = listOf(
        LatLng(35.146421, 129.006428),
        LatLng(35.145915, 129.006488),
        LatLng(35.145510, 129.006725),
        LatLng(35.145694, 129.007742),
        LatLng(35.146064, 129.007747),
        LatLng(35.146178, 129.008721),
        LatLng(35.146597, 129.008568)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // 💡 view_only 모드 여부 확인
        val isViewOnly = intent.getBooleanExtra("view_only", false)

        // 🔒 view_only 모드가 아닌 경우에만 키오스크 모드 진입
        if (!isViewOnly && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
        }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val btnBack = findViewById<Button>(R.id.btn_back_to_admin)
        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        val polygonOptions = PolygonOptions()
            .addAll(polygonCoords)
            .strokeColor(Color.RED)
            .strokeWidth(6f)
            .fillColor(Color.argb(80, 255, 0, 0))
        googleMap.addPolygon(polygonOptions)

        // 카메라를 폴리곤 중앙으로 이동
        val center = polygonCoords[0]
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 17f))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    // 생명주기
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
