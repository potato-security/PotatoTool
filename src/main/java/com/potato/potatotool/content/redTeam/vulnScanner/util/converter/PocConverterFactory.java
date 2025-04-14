package com.potato.potatotool.content.redTeam.vulnScanner.util.converter;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.*;

/**
 * @author Potato
 * @date 2025/3/19 16:30
 * POC转换器工厂类，用于获取不同格式POC的转换器
 */
public class PocConverterFactory {
    
    private static final NucleiPocConverter nucleiConverter = new NucleiPocConverter();
    private static final XrayPocConverter xrayConverter = new XrayPocConverter();
    private static final GobyPocConverter gobyConverter = new GobyPocConverter();
    private static final PocsuitePocConverter pocsuiteConverter = new PocsuitePocConverter();
    
    /**
     * 获取Nuclei格式POC转换器
     * @return Nuclei格式POC转换器
     */
    public static IPocConverter<NucleiYamlObj.Poc> getNucleiConverter() {
        return nucleiConverter;
    }
    
    /**
     * 获取Xray格式POC转换器
     * @return Xray格式POC转换器
     */
    public static IPocConverter<XrayYamlObj.Poc> getXrayConverter() {
        return xrayConverter;
    }
    
    /**
     * 获取Goby格式POC转换器
     * @return Goby格式POC转换器
     */
    public static IPocConverter<GobyJsonObj.PocJson> getGobyConverter() {
        return gobyConverter;
    }
    
    /**
     * 获取Pocsuite格式POC转换器
     * @return Pocsuite格式POC转换器
     */
    public static IPocConverter<PocsuiteJsonObj.PocJson> getPocsuiteConverter() {
        return pocsuiteConverter;
    }
    
    /**
     * 将Nuclei YAML POC转换为通用PocObj
     * @param nucleiPoc Nuclei YAML POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromNuclei(NucleiYamlObj.Poc nucleiPoc) {
        return nucleiConverter.convert(nucleiPoc);
    }
    
    /**
     * 将Xray YAML POC转换为通用PocObj
     * @param xrayPoc Xray YAML POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromXray(XrayYamlObj.Poc xrayPoc) {
        return xrayConverter.convert(xrayPoc);
    }
    
    /**
     * 将Goby JSON POC转换为通用PocObj
     * @param gobyPoc Goby JSON POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromGoby(GobyJsonObj.PocJson gobyPoc) {
        return gobyConverter.convert(gobyPoc);
    }
    
    /**
     * 将Pocsuite JSON POC转换为通用PocObj
     * @param pocsuitePoc Pocsuite JSON POC对象
     * @return 通用PocObj对象
     */
    public static PocObj.Poc fromPocsuite(PocsuiteJsonObj.PocJson pocsuitePoc) {
        return pocsuiteConverter.convert(pocsuitePoc);
    }
}