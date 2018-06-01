package com.qrcore.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: luqihua
 * Time: 2017/7/11
 * Description: QRUtil
 */

public class QRUtil {

    private static final int QR_BM_SIZE = 400;
    private static final int QR_BM_LOGO_SIZE = QR_BM_SIZE / 3;


    public static Bitmap createQRBitmap(String context, int bm_size) {
        return createQRBitmap(context, bm_size, null, 2);
    }

    /**
     * 生成二维码
     *
     * @param context
     * @param bm_size
     * @param logo
     * @param edgeMargin
     * @return
     */
    public static Bitmap createQRBitmap(String context, int bm_size, Bitmap logo, int edgeMargin) {

        Bitmap bitmap = null;
        BitMatrix matrix = null;
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            //设置格式
            Map<EncodeHintType, Object> encodeHintTypeMap = new HashMap<>();
            encodeHintTypeMap.put(EncodeHintType.MARGIN, edgeMargin <= 0 ? 2 : edgeMargin);
            matrix = writer.encode(context, BarcodeFormat.QR_CODE, bm_size, bm_size, encodeHintTypeMap);

            BarcodeEncoder encoder = new BarcodeEncoder();
            bitmap = encoder.createBitmap(matrix);
            if (logo != null && !logo.isRecycled()) {
                bitmap = synthesisLogo(bitmap, logo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /**
     * 在二维码bitmap中加入logo
     *
     * @param qrBm
     * @param logo
     * @return
     */
    private static Bitmap synthesisLogo(Bitmap qrBm, Bitmap logo) {

        int w = logo.getWidth();
        int h = logo.getHeight();

        float wScale = QR_BM_LOGO_SIZE * 1.0f / w;
        float hScale = QR_BM_LOGO_SIZE * 1.0f / h;

        Matrix matrix = new Matrix();
        matrix.postScale(wScale, hScale);
        logo = Bitmap.createBitmap(logo, 0, 0, w, h, matrix, false);


        Canvas canvas = new Canvas(qrBm);

        int middle = qrBm.getWidth() / 2;
        RectF rectF = new RectF(middle - QR_BM_LOGO_SIZE / 2, middle - QR_BM_LOGO_SIZE / 2, middle + QR_BM_LOGO_SIZE / 2, middle + QR_BM_LOGO_SIZE / 2);

        canvas.drawBitmap(logo, null, rectF, null);

        return qrBm;
    }


    /**
     * 识别bitmap中的二维码信息
     *
     * @param bitmap
     * @return
     */
    public static Result spotQRCode(Bitmap bitmap) throws FormatException, ChecksumException, NotFoundException {

        if (bitmap == null || bitmap.isRecycled()) return null;

        Result result = null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] data = new int[width * height];

        bitmap.getPixels(data, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, data);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        QRCodeReader reader = new QRCodeReader();
        //result中包含了扫描到的信息，调用 result.getText()可以获取到文本信息
        result = reader.decode(binaryBitmap);

        bitmap.recycle();

        return result;
    }


}
