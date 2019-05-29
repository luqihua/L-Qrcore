package com.journeyapps.barcodescanner;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.qrcore.util.QRSpotHelper;

import lu.zxingandroid.R;


/**
 *
 */
public class CaptureActivity extends Activity implements DecoratedBarcodeView.TorchListener {

    public static final int SPOT_SUCCESS = 0xeeff00;

    private CaptureManager mCapture;
    private DecoratedBarcodeView mBarcodeScannerView;

    private ImageView mSwitchLightView;
    private ImageView mOpenAlbumView;

    private boolean isLightOn;

    private QRSpotHelper mQrSpotHelper;
    private ContentLoadingProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zxing_capture);
        initView();
        initCaptureManager(savedInstanceState);
        initListener();
    }

    /**
     * 重要方法
     *
     * @param savedInstanceState
     */
    private void initCaptureManager(Bundle savedInstanceState) {
        //初始化配置扫码界面
        mCapture = new CaptureManager(this, mBarcodeScannerView);
        //intent中携带了通过IntentIntegrator设置的参数
        mCapture.initializeFromIntent(getIntent(), savedInstanceState);
        mCapture.decode();
    }

    private void initView() {
        mBarcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        mBarcodeScannerView.getBarcodeView().setFramingRectSize(calculatorFramingRectSize());

        mSwitchLightView = findViewById(R.id.btn_switch_light);
        mOpenAlbumView = findViewById(R.id.btn_open_album);
        mProgressBar = findViewById(R.id.progress);

        if (!hasFlash()) {
            mSwitchLightView.setVisibility(View.GONE);
        }

        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * 设置默认的扫码框大小，如果用户在Intent中传递了size，在initCaptureManager方法中会再次设置
     *
     * @return
     */
    private Size calculatorFramingRectSize() {

        DisplayMetrics outMetrics = Resources.getSystem().getDisplayMetrics();
        int realW, realH;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏
            float w_h_ratio = outMetrics.widthPixels * 1.0f / outMetrics.heightPixels;
            realW = outMetrics.widthPixels * 3 / 5;
            realH = (int) (realW / w_h_ratio);
        } else {
            realH = realW = outMetrics.widthPixels * 3 / 5;
        }
        return new Size(realW, realH);
    }

    private void initListener() {
        mBarcodeScannerView.setTorchListener(this);

        mSwitchLightView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLightOn) {
                    mBarcodeScannerView.setTorchOff();
                } else {
                    mBarcodeScannerView.setTorchOn();
                }
            }
        });

        //相册选取按钮的点击事件
        mOpenAlbumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mQrSpotHelper == null) {
                    mQrSpotHelper = new QRSpotHelper(CaptureActivity.this, mOnSpotCallBack);
                }
                mQrSpotHelper.spotFromAlbum();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCapture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCapture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCapture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCapture.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        mCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mBarcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onTorchOn() {
        isLightOn = true;
    }

    @Override
    public void onTorchOff() {
        isLightOn = false;
    }

    // 判断是否有闪光灯功能
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mQrSpotHelper != null) {
            mQrSpotHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    //照片识别的监听
    private QRSpotHelper.OnSpotCallBack mOnSpotCallBack = new QRSpotHelper.OnSpotCallBack() {
        @Override
        public void onSpotStart() {
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onSpotSuccess(Result result) {
            //识别成功后将返回的结果传递给上层activity
            mProgressBar.setVisibility(View.GONE);
            String data = result.getText();
            Intent intent = new Intent();
            intent.putExtra("data", data);
            setResult(SPOT_SUCCESS, intent);
            finish();
        }

        @Override
        public void onSpotError() {
            mProgressBar.setVisibility(View.GONE);
            Toast.makeText(CaptureActivity.this, "未发现二维码", Toast.LENGTH_SHORT).show();
        }
    };
}
