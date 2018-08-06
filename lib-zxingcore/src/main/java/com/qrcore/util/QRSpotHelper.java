package com.qrcore.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;

/**
 * Author: luqihua
 * Time: 2017/7/12
 * Description: 从相册选取图片并识别二维码
 */

public class QRSpotHelper {
    private static final int READ_BM_SIZE = 600;//bitmap图片加载到内存的最大尺寸
    private static final int READ_OUT_TIME = 4000;//识别超时时间
    private static final int CHECK_PHOTO = 0x2221;

    private static final int HANDLER_START = 0x10;
    private static final int HANDLER_SUCCESS = 0x11;
    private static final int HANDLER_ERROR = 0x12;


    private Activity mActivity;
    private OnSpotCallBack mOnSpotCallBack;
    private SpotThread mSpotThread;

    private SpotHandler mHandler;


    public QRSpotHelper(@NonNull Activity activity, OnSpotCallBack callBack) {
        this.mActivity = activity;
        this.mOnSpotCallBack = callBack;
        mHandler = new SpotHandler(this);
    }

    /**
     * 相册选取二维码照片并识别
     */
    public void spotFromAlbum() {
        if (mSpotThread != null && !mSpotThread.isCancel()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
        mActivity.startActivityForResult(intent, CHECK_PHOTO);
    }


    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHECK_PHOTO) {
            final Uri uri = data.getData();
            final Bitmap bitmap = getBmFromUri(uri);
            mSpotThread = new SpotThread(mHandler, bitmap);
            mSpotThread.start();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(HANDLER_ERROR);
                }
            }, READ_OUT_TIME);
        }
    }

    private Bitmap getBmFromUri(Uri uri) {
        Bitmap result = null;
        InputStream in = null;
        try {
            in = mActivity.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);

            int w = options.outWidth;
            int h = options.outHeight;

            int ratio = 1;
            if (w > READ_BM_SIZE || h > READ_BM_SIZE) {
                int wRatio = (int) Math.ceil(w / READ_BM_SIZE);
                int hRatio = (int) Math.ceil(h / READ_BM_SIZE);
                ratio = Math.min(wRatio, hRatio);
            }
            options.inSampleSize = ratio;
            options.inJustDecodeBounds = false;
            //需要重新打开输入流
            in = mActivity.getContentResolver().openInputStream(uri);
            result = BitmapFactory.decodeStream(in, null, options);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    /**
     * 进行二维码识别的线程
     */
    private static class SpotThread extends Thread {

        private Handler handler;
        private boolean isCancel;
        private Bitmap codeBm;

        public SpotThread(Handler handler, Bitmap bitmap) {
            this.handler = handler;
            this.codeBm = bitmap;
        }


        public void setCancel(boolean cancel) {
            isCancel = cancel;
        }

        public boolean isCancel() {
            return isCancel;
        }

        @Override
        public synchronized void start() {
            super.start();
            handler.sendEmptyMessage(HANDLER_START);
        }

        @Override
        public void run() {
            Result result = null;
            try {
                result = QRUtil.spotQRCode(codeBm);
            } catch (FormatException | ChecksumException | NotFoundException e) {
                e.printStackTrace();
            }

            if (!isCancel) {
                isCancel = true;
                handler.obtainMessage(HANDLER_SUCCESS, result).sendToTarget();
            }

        }
    }

    /**
     * 二维码识别结果的handler
     */
    private static class SpotHandler extends Handler {

        private WeakReference<QRSpotHelper> weakReference;

        public SpotHandler(QRSpotHelper helper) {
            this.weakReference = new WeakReference<>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            QRSpotHelper helper = weakReference.get();
            if (helper == null) return;

            switch (msg.what) {
                case HANDLER_START:
                    helper.mOnSpotCallBack.onSpotStart();
                    break;
                case HANDLER_SUCCESS:

                    Result result = (Result) msg.obj;
                    if (result != null) {
                        helper.mOnSpotCallBack.onSpotSuccess(result);
                    } else {
                        helper.mOnSpotCallBack.onSpotError();
                    }

                    break;
                case HANDLER_ERROR:
                    if (helper.mSpotThread != null && !helper.mSpotThread.isCancel()) {
                        helper.mSpotThread.setCancel(true);
                    }
                    helper.mOnSpotCallBack.onSpotError();
                    break;
            }
        }
    }

    /**
     * 识别回调
     */
    public interface OnSpotCallBack {
        void onSpotStart();

        void onSpotSuccess(Result result);

        void onSpotError();
    }

}
