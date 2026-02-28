package com.potato.potatotool.content.redTeam.vulnScanner.core.executor;

import com.potato.potatotool.content.redTeam.vulnScanner.classObj.PocObj;
import com.potato.potatotool.content.redTeam.vulnScanner.http.DnsHandler;
import com.potato.potatotool.content.redTeam.vulnScanner.http.HttpHandler;

import java.util.Map;

/**
 * DNS 协议执行器
 * 负责执行 DNS 查询类型的 POC 步骤
 * 
 * 功能：
 * 1. 执行 DNS 查询（A、AAAA、CNAME、MX、TXT等记录类型）
 * 2. 支持自定义 DNS 解析器
 * 3. 支持 DNS 响应匹配
 * 
 * @author Potato
 * @date 2025-11-26
 */
public class DnsProtocolExecutor implements IProtocolExecutor<PocObj.DnsStep> {
    
    private static final String PROTOCOL = "dns";
    
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }
    
    @Override
    public boolean supports(PocObj.PocStep step) {
        return step instanceof PocObj.DnsStep;
    }
    
    @Override
    public ExecutionResult execute(PocObj.DnsStep dnsStep, Map<String, String> variables, 
                                   ExecutionContext context) {
        try {
            System.out.println("→ 执行 DNS 查询: " + dnsStep.getDomain());
            
            // 替换变量（支持嵌套变量）
            String domain = HttpHandler.replaceVariables(dnsStep.getDomain(), variables);
            
            // 确定记录类型
            String recordType = dnsStep.getType() != null ? dnsStep.getType() : "A";
            
            // 执行 DNS 查询
            DnsHandler.DnsResponse dnsResponse = DnsHandler.query(
                domain,
                recordType,
                dnsStep.getResolver(),
                true,
                2
            );
            
            if (!dnsResponse.isSuccess()) {
                System.err.println("DNS 查询失败: " + dnsResponse.getError());
                return ExecutionResult.failure("DNS 查询失败: " + dnsResponse.getError());
            }
            
            // 构造可匹配的响应体
            String responseBody = String.join("\n", dnsResponse.getAnswers());
            
            // 执行匹配
            if (dnsStep.getMatchers() != null && !dnsStep.getMatchers().isEmpty()) {
                boolean matched = matchDnsResponse(responseBody, dnsResponse, dnsStep);
                if (!matched) {
                    System.out.println("DNS 响应不匹配");
                    return ExecutionResult.failure("DNS 响应不匹配");
                }
            }
            
            System.out.println("✓ DNS 步骤执行成功");
            return ExecutionResult.success("DNS 查询成功: " + domain);
            
        } catch (Exception e) {
            System.err.println("DNS 步骤执行失败: " + e.getMessage());
            return ExecutionResult.failure("DNS 步骤执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 匹配 DNS 响应
     */
    private boolean matchDnsResponse(String responseBody, DnsHandler.DnsResponse dnsResponse, 
                                     PocObj.PocStep step) {
        for (PocObj.Matcher matcher : step.getMatchers()) {
            boolean matcherResult = matchSingleMatcher(matcher, responseBody, dnsResponse);
            
            // 根据匹配条件决定继续或返回
            if (step.getMatchersCondition() == PocObj.MatchersCondition.OR) {
                if (matcherResult) {
                    return true; // OR 条件下，任一匹配成功即返回
                }
            } else {
                // AND 条件（默认）
                if (!matcherResult) {
                    return false; // AND 条件下，任一失败即返回
                }
            }
        }
        
        // OR 条件下全部失败返回 false，AND 条件下全部成功返回 true
        return step.getMatchersCondition() != PocObj.MatchersCondition.OR;
    }
    
    /**
     * 匹配单个匹配器
     */
    private boolean matchSingleMatcher(PocObj.Matcher matcher, String responseBody, 
                                       DnsHandler.DnsResponse dnsResponse) {
        if (matcher.getValues() == null || matcher.getValues().isEmpty()) {
            return true;
        }
        
        switch (matcher.getType()) {
            case WORD:
                return matchWord(responseBody, matcher);
            case REGEX:
                return matchRegex(responseBody, matcher);
            case STATUS:
                return dnsResponse.isSuccess();
            default:
                System.err.println("DNS 不支持的匹配类型: " + matcher.getType());
                return false;
        }
    }
    
    /**
     * Word 匹配
     */
    private boolean matchWord(String content, PocObj.Matcher matcher) {
        boolean isAnd = "AND".equalsIgnoreCase(matcher.getCondition());
        
        for (String word : matcher.getValues()) {
            boolean contains = content.contains(word);
            
            if (isAnd && !contains) {
                return false;
            }
            if (!isAnd && contains) {
                return true;
            }
        }
        
        return isAnd; // AND 返回 true，OR 返回 false
    }
    
    /**
     * Regex 匹配
     */
    private boolean matchRegex(String content, PocObj.Matcher matcher) {
        boolean isAnd = "AND".equalsIgnoreCase(matcher.getCondition());
        
        for (String regex : matcher.getValues()) {
            try {
                boolean matches = content.matches("(?s).*" + regex + ".*");
                
                if (isAnd && !matches) {
                    return false;
                }
                if (!isAnd && matches) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("正则表达式错误: " + regex + " - " + e.getMessage());
                if (isAnd) {
                    return false;
                }
            }
        }
        
        return isAnd;
    }
}
