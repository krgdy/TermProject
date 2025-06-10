package com.example.termproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.termproject.databinding.ActivityMapBinding

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var locationPermissionGranted = false

    companion object {
        private const val TAG = "test : Mapactivity"
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val DEFAULT_ZOOM = 15f
        private val DEFAULT_LOCATION = LatLng(35.1731, 129.0714) // 부산, 기본 위치
    }


    private lateinit var binding: ActivityMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // SupportMapFragment를 가져와 지도가 준비되면 알림을 받습니다.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // FusedLocationProviderClient 초기화
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * 지도를 사용할 준비가 되면 호출되는 콜백입니다.
     * 이 콜백이 트리거될 때까지 지도의 핸들을 가져올 수 없습니다.
     * 여기에서 마커를 추가하거나, 선을 추가하거나, 리스너를 설정하는 등의 작업을 수행합니다.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission()
        }
        else locationPermissionGranted = true
        // 위치 UI 및 현재 위치 가져오기
        updateLocationUI()
        getDeviceLocation()
    }

    /**
     * 기기 위치를 가져오기 위한 권한을 요청합니다.
     */
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    /**
     * 권한 요청 결과에 대한 콜백입니다.
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    /**
     * 지도 UI 설정 (내 위치 버튼 등)을 업데이트합니다.
     */
    @SuppressLint("MissingPermission") // 권한 검사를 이미 했으므로 린트 경고 억제
    private fun updateLocationUI() {
        if (!::mMap.isInitialized) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                lastKnownLocation = null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exception: %s".format(e.message))
        }
    }

    /**
     * 기기의 가장 최근 위치를 가져와 지도의 카메라를 해당 위치로 설정합니다.
     */
    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult: Task<Location> = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 지도의 카메라 위치를 기기의 현재 위치로 설정합니다.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM))
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s".format(task.exception))
                            mMap.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
                            mMap.uiSettings.isMyLocationButtonEnabled = false
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s".format(task.exception))
                        mMap.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exception: %s".format(e.message))
        }
    }
}