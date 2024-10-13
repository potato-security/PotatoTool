package com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * @author Potato
 * @date 2024/10/9 10:19
 * @desc 图片感知哈希算法（pHash）通过计算图片的感知哈希值并比较哈希值之间的汉明距离来判断图片相似度
 */
public class ImgPHsh {
    private int size = 32;            // 默认DCT处理的图像大小为32x32
    private int smallerSize = 8;      // 默认保留的DCT较低频部分为8x8
    private double[] c;               // DCT系数数组
    private ColorConvertOp colorConvert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    /**
     * 构造方法，初始化DCT系数
     */
    public ImgPHsh() {
        initCoefficients();
    }

    /**
     * 构造方法，允许自定义图像大小和保留的低频区域大小
     * @param size 图像大小
     * @param smallerSize 保留的低频区域大小
     */
    public ImgPHsh(int size, int smallerSize) {
        this.size = size;
        this.smallerSize = smallerSize;
        initCoefficients();
    }

    /**
     * 初始化DCT系数
     */
    private void initCoefficients() {
        c = new double[size];
        for (int i = 1; i < size; i++) {
            c[i] = 1;
        }
        c[0] = 1 / Math.sqrt(2.0);
    }

    /**
     * 计算汉明距离，用于比较两个图片的pHash值
     * @param s1 第一个哈希字符串
     * @param s2 第二个哈希字符串
     * @return 汉明距离（值越小，相似度越高）
     */
    private int calculateHammingDistance(String s1, String s2) {
        int distance = 0;
        for (int k = 0; k < s1.length(); k++) {
            if (s1.charAt(k) != s2.charAt(k)) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * 生成图片的pHash值
     * @param is 输入图片流
     * @return 图片的pHash值（二进制字符串）
     * @throws Exception 处理图像时可能抛出的异常
     */
    private String getHash(InputStream is) throws Exception {
        BufferedImage img = ImageIO.read(is);

        // 步骤1：调整图像尺寸为size x size（默认为32x32）
        img = resize(img, size, size);

        // 步骤2：将图像转为灰度图
        img = grayscale(img);

        // 步骤3：获取图像的DCT值
        double[][] dctValues = calculateDCT(img);

        // 步骤4：仅保留左上角的8x8低频DCT值
        // 步骤5：计算均值（排除[0,0]元素）
        double avg = calculateDCTAverage(dctValues);

        // 步骤6：生成二进制哈希字符串
        return generateHash(dctValues, avg);
    }
    private String getHash(byte[] imageBytes) throws Exception {
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage img = ImageIO.read(is);
        img = resize(img, size, size);
        img = grayscale(img);
        double[][] dctValues = calculateDCT(img);
        double avg = calculateDCTAverage(dctValues);
        return generateHash(dctValues, avg);
    }

    /**
     * 调整图片尺寸
     * @param image 原图像
     * @param width 目标宽度
     * @param height 目标高度
     * @return 调整后的图像
     */
    private BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * 将图像转为灰度图
     * @param img 原图像
     * @return 灰度图像
     */
    private BufferedImage grayscale(BufferedImage img) {
        colorConvert.filter(img, img);
        return img;
    }

    /**
     * 计算DCT转换后的值
     * @param img 灰度图像
     * @return DCT值矩阵
     */
    private double[][] calculateDCT(BufferedImage img) {
        double[][] pixelValues = new double[size][size];

        // 获取图像像素值
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                pixelValues[x][y] = getBlue(img, x, y);
            }
        }

        // 应用DCT转换
        return applyDCT(pixelValues);
    }

    /**
     * 获取图像中某像素点的蓝色值（灰度图中只有蓝色通道有值）
     * @param img 图像
     * @param x 像素点x坐标
     * @param y 像素点y坐标
     * @return 该像素点的蓝色值
     */
    private static int getBlue(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) & 0xff;
    }

    /**
     * 计算DCT的平均值，排除[0,0]位置
     * @param dctValues DCT值矩阵
     * @return 平均值
     */
    private double calculateDCTAverage(double[][] dctValues) {
        double total = 0;
        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                total += dctValues[x][y];
            }
        }
        total -= dctValues[0][0]; // 排除DC分量
        return total / ((smallerSize * smallerSize) - 1);
    }

    /**
     * 生成pHash值（二进制字符串）
     * @param dctValues DCT值矩阵
     * @param avg DCT均值
     * @return 二进制哈希字符串
     */
    private String generateHash(double[][] dctValues, double avg) {
        StringBuilder hash = new StringBuilder();
        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                if (x != 0 || y != 0) {
                    hash.append(dctValues[x][y] > avg ? "1" : "0");
                }
            }
        }
        return hash.toString();
    }

    /**
     * 计算两个图片之间的相似度（0到1.0）
     * @param srcFile 源图像文件
     * @param canFile 候选图像文件
     * @return 相似度（0到1.0）
     * @throws Exception 处理图像时可能抛出的异常
     */
    public double match(File srcFile, File canFile) throws Exception {
        String srcHash = getHash(new FileInputStream(srcFile));
        String canHash = getHash(new FileInputStream(canFile));
        int hammingDistance = calculateHammingDistance(srcHash, canHash);
        return 1.0 - (double) hammingDistance / (smallerSize * smallerSize - 1);
    }

    /**
     * 计算两个图片之间的相似度（0到1.0）
     * @param srcUrl 源图像URL
     * @param canUrl 候选图像URL
     * @return 相似度（0到1.0）
     * @throws Exception 处理图像时可能抛出的异常
     */
    public double match(URL srcUrl, URL canUrl) throws Exception {
        String srcHash = getHash(srcUrl.openStream());
        String canHash = getHash(canUrl.openStream());
        int hammingDistance = calculateHammingDistance(srcHash, canHash);
        return 1.0 - (double) hammingDistance / (smallerSize * smallerSize - 1);
    }

    /**
     * 计算两个图片之间的相似度（0到1.0）
     * @param srcImg 源图像byte[]
     * @param canImg 候选图像byte[]
     * @return 相似度（0到1.0）
     * @throws Exception 处理图像时可能抛出的异常
     */
    public double match(byte[] srcImg, byte[] canImg) throws Exception {
        String srcHash = getHash(srcImg);
        String canHash = getHash(canImg);
        int hammingDistance = calculateHammingDistance(srcHash, canHash);
        return 1.0 - (double) hammingDistance / (smallerSize * smallerSize - 1);
    }

    public int calculateImageDistance(byte[] srcImg, byte[] canImg) throws Exception {
        String srcHash = getHash(srcImg);
        String canHash = getHash(canImg);
        return calculateHammingDistance(srcHash, canHash);
    }

    /**
     * 离散余弦变换（DCT）算法
     * @param pixelValues 图像像素值矩阵
     * @return DCT值矩阵
     */
    private double[][] applyDCT(double[][] pixelValues) {
        int N = size;
        double[][] DCT = new double[N][N];

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += Math.cos(((2 * i + 1) / (2.0 * N)) * u * Math.PI) *
                                Math.cos(((2 * j + 1) / (2.0 * N)) * v * Math.PI) *
                                pixelValues[i][j];
                    }
                }
                sum *= ((c[u] * c[v]) / 4.0);
                DCT[u][v] = sum;
            }
        }
        return DCT;
    }
}
