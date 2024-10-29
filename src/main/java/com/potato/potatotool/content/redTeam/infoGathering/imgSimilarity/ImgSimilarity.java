package com.potato.potatotool.content.redTeam.infoGathering.imgSimilarity;

import com.potato.potatotool.content.redTeam.infoGathering.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/10/9 15:33
 * @desc 该类用于结合 ImgPHsh 和 ImgHistogram 两种方法，提高图片相似度判断的准确率
 */
public class ImgSimilarity {

    private ImgHistogram imgHistogram;
    private ImgPHsh imgPHsh;

    // 权重设置，用于综合两种相似度
    private double histogramWeight = 0.5;  // 直方图相似度的权重
    private double phashWeight = 0.5;      // pHash相似度的权重
    private double histogramThreshold = 0.8;  // 直方图相似度的阈值
    private double phashThreshold = 0.8;      // pHash相似度的阈值

    // 默认构造函数，初始化直方图和pHash
    public ImgSimilarity() {
        this.imgHistogram = new ImgHistogram();
        this.imgPHsh = new ImgPHsh();
    }

    // 可以自定义权重的构造函数
    public ImgSimilarity(double histogramWeight, double phashWeight) {
        this();
        this.histogramWeight = histogramWeight;
        this.phashWeight = phashWeight;
    }

    /**
     * 比较两张图片的相似度，结合直方图和pHash
     * @param srcFile 源图片文件
     * @param canFile 候选图片文件
     * @return 综合的相似度
     * @throws IOException 如果文件读取异常
     */
    public double match(URL srcFile, URL canFile) throws Exception {
        // 获取直方图相似度
        double histogramSimilarity = imgHistogram.match(srcFile, canFile);

        // 获取pHash相似度
        double pHshSimilarity = imgPHsh.match(srcFile, canFile);

        // 综合相似度
        return histogramWeight * histogramSimilarity + phashWeight * pHshSimilarity;
    }
    public double match(File srcFile, File canFile) throws Exception {
        // 获取直方图相似度
        double histogramSimilarity = imgHistogram.match(srcFile, canFile);

        // 获取pHash相似度
        double pHshSimilarity = imgPHsh.match(srcFile, canFile);

        // 综合相似度
        return histogramWeight * histogramSimilarity + phashWeight * pHshSimilarity;
    }
    public double match(byte[] srcFile, byte[] canFile) throws Exception {
        // 获取直方图相似度
        double histogramSimilarity = imgHistogram.match(srcFile, canFile);

        // 获取pHash相似度
        double pHshSimilarity = imgPHsh.match(srcFile, canFile);

        // 综合相似度
        return histogramWeight * histogramSimilarity + phashWeight * pHshSimilarity;
    }

    // 根据阈值判断是否相似
    public boolean matchSimilar(byte[] srcFile, byte[] canFile) throws Exception {
        // 获取直方图相似度
        double histogramSimilarity = imgHistogram.match(srcFile, canFile);

        // 获取pHash相似度
        double pHshSimilarity = imgPHsh.match(srcFile, canFile);

        return (histogramSimilarity > histogramThreshold || pHshSimilarity > phashThreshold);
    }
    public boolean matchSimilar(URL srcURL, URL canURL) throws Exception {
        // 获取直方图相似度
        double histogramSimilarity = imgHistogram.match(srcURL, canURL);

        // 获取pHash相似度
        double pHshSimilarity = imgPHsh.match(srcURL, canURL);

        return (histogramSimilarity > histogramThreshold || pHshSimilarity > phashThreshold);
    }

    // 设置直方图相似度的权重
    public void setHistogramWeight(double histogramWeight) {
        this.histogramWeight = histogramWeight;
    }

    // 设置pHash相似度的权重
    public void setPhashWeight(double phashWeight) {
        this.phashWeight = phashWeight;
    }

    public static void main(String[] args) throws Exception {

        Map<String, Object> webInfoMap = Utils.getWebInfo("https://211.160.72.129");
        System.out.println(webInfoMap);
        byte[] srcFile = Base64.getDecoder().decode(webInfoMap.get("iconBase64").toString());
        Map<String, Object> webInfoMap1 = Utils.getWebInfo("https://43.143.141.199:8443");
        System.out.println(webInfoMap1);
        byte[] canFile = Base64.getDecoder().decode(webInfoMap1.get("iconBase64").toString());
        boolean similarity = new ImgSimilarity().matchSimilar(srcFile, canFile);
        System.out.println("是否相似: " + similarity);


        double similarity1 = new ImgSimilarity().match(new URL("https://t8.baidu.com/it/u=3036650915,1842869833&fm=193"), new URL("https://t9.baidu.com/it/u=140484125,2114791292&fm=193"));
        System.out.println("综合相似度: " + similarity1);
    }
}
