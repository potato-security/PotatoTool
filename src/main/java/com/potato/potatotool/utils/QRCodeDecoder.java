package com.potato.potatotool.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
/**
 * @author Potato
 * @date 2024/7/16 20:04
 */
public class QRCodeDecoder {

    public static String qrToText(String pathStr) {
        String qrText = "读取错误";
        try {
            File file = new File(pathStr);
            BufferedImage image = ImageIO.read(file);
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result qrCodeResult = new MultiFormatReader().decode(bitmap);
            qrText = qrCodeResult.getText();
        } catch (Exception e) {}
        return qrText;
    }

}