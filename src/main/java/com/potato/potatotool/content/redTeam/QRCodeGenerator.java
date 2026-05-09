package com.potato.potatotool.content.redTeam;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/9/11 15:20
 */
public class QRCodeGenerator {
    public static void generateQRCodeImageWithLogo(String text, int width, int height, int logoWidth, int logoHeight, String filePath, String logoPath)
            throws WriterException, IOException {
        // logo默认为二维码宽高的1/5
        if(logoWidth < 1) logoWidth = width / 5;
        if(logoHeight < 1) logoHeight = height / 5;

        // 配置二维码的编码参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0); // 设置二维码边距为 0

        // 生成二维码矩阵
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        // 设置二维码的颜色配置，保留黑白色调
        MatrixToImageConfig config = new MatrixToImageConfig(Color.BLACK.getRGB(), Color.WHITE.getRGB());

        // 将二维码转换为BufferedImage
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

        // 如果提供了logo路径，则添加logo到二维码中
        if (logoPath != null && !logoPath.isEmpty()) {
            // 读取logo图片
            BufferedImage logoImage = ImageIO.read(new File(logoPath));
            if (logoImage != null) {
                // 确保 logo 图像是透明背景的
                BufferedImage logoWithAlpha = new BufferedImage(logoImage.getWidth(), logoImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = logoWithAlpha.createGraphics();
                g2d.drawImage(logoImage, 0, 0, null);
                g2d.dispose();

                // 计算logo在二维码中间的位置
                int logoX = (qrImage.getWidth() - logoWidth) / 2;
                int logoY = (qrImage.getHeight() - logoHeight) / 2;

                // 创建一个新的图像来绘制二维码和 logo
                BufferedImage finalImage = new BufferedImage(qrImage.getWidth(), qrImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = finalImage.createGraphics();
                // 抗锯齿处理，保证绘图质量
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 绘制二维码
                g.drawImage(qrImage, 0, 0, null);
                // 中心绘制logo
                g.drawImage(logoWithAlpha, logoX, logoY, logoWidth, logoHeight, null);
                // 释放Graphics2D对象
                g.dispose();

                // 将带logo的二维码保存为图片文件
                Path path = FileSystems.getDefault().getPath(filePath);
                ImageIO.write(finalImage, "PNG", path.toFile());
            } else {
                // 如果无法读取logo图像，则仅保存二维码
                Path path = FileSystems.getDefault().getPath(filePath);
                ImageIO.write(qrImage, "PNG", path.toFile());
            }
        } else {
            // 如果没有提供logo路径，则仅保存二维码
            Path path = FileSystems.getDefault().getPath(filePath);
            ImageIO.write(qrImage, "PNG", path.toFile());
        }
    }

    public static void main(String[] args) {
        try {
            // 定义二维码内容和保存路径
            String text = "https://www.baidu.com";
            String filePath = "二维码.png";
            String logoPath = "/Users/potato/Documents/123.jpg";
            int width = 300;
            int height = 300;
            int logoWidth = 300/5;
            int logoHeight = 300/5;

            // 生成带有logo的二维码
            generateQRCodeImageWithLogo(text, width, height, logoWidth, logoHeight, filePath, logoPath);
        } catch (WriterException | IOException e) {
            System.out.println("二维码生成失败: " + e.getMessage());
        }
    }
}
