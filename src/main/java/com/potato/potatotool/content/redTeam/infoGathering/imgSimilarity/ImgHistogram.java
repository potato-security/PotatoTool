package com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * @author Potato
 * @date 2024/10/9 10:04
 * @desc 该类用于计算和比较图像的直方图相似度（使用巴氏系数）
 */
public class ImgHistogram {
    // 颜色最大值255，用于归一化计算
    private static final int COLOR_MAX_VALUE = 255;
    // 红、绿、蓝通道的bin数量
    private int redBins;
    private int greenBins;
    private int blueBins;

    // 默认构造函数，初始化bin数量为4
    public ImgHistogram() {
        this(4, 4, 4); // 默认使用4个bin
    }

    // 可以自定义红、绿、蓝bin数量的构造函数
    public ImgHistogram(int redBins, int greenBins, int blueBins) {
        this.redBins = redBins;
        this.greenBins = greenBins;
        this.blueBins = blueBins;
    }

    /**
     * 计算图像的直方图数据
     * @param image 输入的BufferedImage对象
     * @return 归一化的直方图数据
     */
    private float[] calculateHistogram(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        float[] histogramData = new float[redBins * greenBins * blueBins];

        // 获取图像的RGB像素数据
        getRGB(image, 0, 0, width, height, pixels);
        float totalPixels = 0;

        // 遍历图像中的每个像素，计算直方图
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                int red = (pixels[index] >> 16) & 0xff;   // 提取红色分量
                int green = (pixels[index] >> 8) & 0xff;  // 提取绿色分量
                int blue = pixels[index] & 0xff;          // 提取蓝色分量

                // 计算每个分量所在的bin索引
                int redIdx = getBinIndex(redBins, red);
                int greenIdx = getBinIndex(greenBins, green);
                int blueIdx = getBinIndex(blueBins, blue);

                // 计算该像素在直方图数组中的位置
                int histogramIndex = redIdx + greenIdx * redBins + blueIdx * redBins * greenBins;
                histogramData[histogramIndex] += 1;
                totalPixels += 1;
            }
        }

        // 将直方图数据归一化
        for (int i = 0; i < histogramData.length; i++) {
            histogramData[i] /= totalPixels;
        }

        return histogramData;
    }

    /**
     * 将颜色值映射到相应的bin上
     * @param binCount bin的数量
     * @param color 当前的颜色值
     * @return 颜色所在的bin索引
     */
    private int getBinIndex(int binCount, int color) {
        int binIndex = (color * binCount) / COLOR_MAX_VALUE;
        return binIndex >= binCount ? binCount - 1 : binIndex;
    }

    /**
     * 获取图像的RGB值数组
     * @param image 输入的BufferedImage对象
     * @param x 开始的x坐标
     * @param y 开始的y坐标
     * @param width 宽度
     * @param height 高度
     * @param pixels 用于存储RGB值的像素数组
     * @return 填充了RGB值的像素数组
     */
    private int[] getRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
        }
        return image.getRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * 计算两张图片直方图的巴氏系数（Bhattacharyya Coefficient）
     * @param histogram1 图像1的直方图
     * @param histogram2 图像2的直方图
     * @return 返回巴氏系数，0到1之间，1表示完全相同
     */
    private double calculateSimilarity(float[] histogram1, float[] histogram2) {
        double similarity = 0;
        for (int i = 0; i < histogram1.length; i++) {
            similarity += Math.sqrt(histogram1[i] * histogram2[i]);
        }
        return similarity;
    }

    /**
     * 比较两张图片文件的相似度
     * @param srcFile 源图片文件
     * @param canFile 候选图片文件
     * @return 两张图片的相似度
     * @throws IOException 文件读取异常
     */
    public double match(File srcFile, File canFile) throws IOException {
        BufferedImage srcImage = ImageIO.read(srcFile);
        BufferedImage canImage = ImageIO.read(canFile);
        return match(srcImage, canImage);
    }

    /**
     * 比较两张图片的相似度（通过URL读取）
     * @param srcUrl 源图片URL
     * @param canUrl 候选图片URL
     * @return 两张图片的相似度
     * @throws IOException URL读取异常
     */
    public double match(URL srcUrl, URL canUrl) throws IOException {
        BufferedImage srcImage = ImageIO.read(srcUrl);
        BufferedImage canImage = ImageIO.read(canUrl);
        return match(srcImage, canImage);
    }

    /**
     * 比较两张图片的相似度（通过byte[]）
     * @param srcBytes 源图片的字节数组
     * @param canBytes 候选图片的字节数组
     * @return 两张图片的相似度
     * @throws IOException 字节数组读取异常
     */
    public double match(byte[] srcBytes, byte[] canBytes) throws IOException {
        BufferedImage srcImage = ImageIO.read(new ByteArrayInputStream(srcBytes));
        BufferedImage canImage = ImageIO.read(new ByteArrayInputStream(canBytes));
        return match(srcImage, canImage);
    }

    /**
     * 比较两张BufferedImage图像的相似度
     * @param srcImage 源图片
     * @param canImage 候选图片
     * @return 两张图片的相似度
     * @throws IOException 如果图像为空，抛出异常
     */
    private double match(BufferedImage srcImage, BufferedImage canImage) throws IOException {
        if (srcImage == null || canImage == null) {
            throw new IllegalArgumentException("Source or candidate image cannot be null.");
        }
        float[] srcHistogram = calculateHistogram(srcImage);
        float[] canHistogram = calculateHistogram(canImage);
        return calculateSimilarity(srcHistogram, canHistogram);
    }
}