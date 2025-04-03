package com.potato.potatotool.content.redTeam.vulnScanner.util;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.NucleiYamlObj;
import com.potato.potatotool.content.redTeam.vulnScanner.util.constructor.NucleiConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author Potato
 * @date 2025/3/12 15:36
 */
public class YamlPocParser{
    public static void parser(String path) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        NucleiConstructor constructor = new NucleiConstructor(NucleiYamlObj.Poc.class, loaderOptions);

        Yaml yaml = new Yaml(constructor);
        try (InputStream input = new FileInputStream(path)) {
            NucleiYamlObj.Poc poc = yaml.loadAs(input, NucleiYamlObj.Poc.class);
            System.out.println("ID: " + poc.getId());
            System.out.println(poc);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


}
