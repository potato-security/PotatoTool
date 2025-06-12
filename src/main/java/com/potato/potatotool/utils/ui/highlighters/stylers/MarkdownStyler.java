package com.potato.potatotool.utils.ui.highlighters.stylers;

import com.potato.potatotool.utils.ui.highlighters.HighlighterConstants;
import com.potato.potatotool.utils.ui.highlighters.MultiLanguageHighlighter;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.*;

/**
 * Markdown语言代码高亮实现
 * 基于CommonMark库解析Markdown语法元素
 * 支持h1-h6标题、加粗、斜体等丰富样式
 * 使用访问者模式处理不同类型的Markdown元素
 * @author Potato
 */
public class MarkdownStyler implements MultiLanguageHighlighter.LanguageStyler {
    
    // 使用CommonMark解析器，配置为包含源代码位置信息
    private static final Parser parser = Parser.builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
            .build();

    @Override
    public Set<String> getLanguageNames() {
        Set<String> names = new HashSet<>();
        names.add("markdown");
        names.add("md");
        return Collections.unmodifiableSet(names);
    }

    @Override
    public StyleSpans<Collection<String>> computeStyles(String text) {
        try {
            // 解析Markdown文档
            Node document = parser.parse(text);
            
            // 创建访问者并应用样式
            StyleSpanVisitor visitor = new StyleSpanVisitor(text);
            document.accept(visitor);
            
            // 构建并返回样式
            return visitor.buildStyles();
        } catch (Exception e) {
            // 出现异常时返回无样式
            StyleSpansBuilder<Collection<String>> errorBuilder = new StyleSpansBuilder<>();
            errorBuilder.add(Collections.emptyList(), text.length());
            return errorBuilder.create();
        }
    }

    /**
     * Markdown元素样式访问者
     * 实现CommonMark的AbstractVisitor，为不同类型的Markdown元素应用样式
     */
    static class StyleSpanVisitor extends AbstractVisitor {

        private final String text;
        private final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        private final int[] lineSums;
        private int lastInd = 0;
        
        // 使用栈结构管理嵌套样式
        private final Deque<String> currentStyle = new ArrayDeque<>();

        StyleSpanVisitor(String text) {
            this.text = text;
            
            // 计算每行的累计长度，用于定位元素位置
            String[] lines = text.split("\\r?\\n");
            int[] lengths = new int[lines.length];
            for (int i = 0; i < lines.length; i++) {
                lengths[i] = lines[i].length() + 1; // +1 for the newline character
            }
            
            lineSums = new int[lengths.length + 1];
            for (int i = 0; i < lengths.length; i++) {
                lineSums[i + 1] = lineSums[i] + lengths[i];
            }
            
            // 添加基础Markdown样式
            currentStyle.add(HighlighterConstants.MD_STYLE);
        }
        
        /**
         * 构建最终的样式集合
         * @return 样式集合
         */
        public StyleSpans<Collection<String>> buildStyles() {
            appendStyle(text.length(), true);
            return spansBuilder.create();
        }
        
        /**
         * 添加样式到指定位置
         * @param untilInd 结束位置
         */
        private void appendStyle(int untilInd) {
            appendStyle(untilInd, false);
        }
        
        /**
         * 添加样式到指定位置
         * @param untilInd 结束位置
         * @param lastStyle 是否为最后一个样式
         */
        private void appendStyle(int untilInd, boolean lastStyle) {
            if (untilInd > lastInd || lastStyle) {
                if (currentStyle.isEmpty()) {
                    spansBuilder.add(Collections.emptyList(), untilInd - lastInd);
                } else if (currentStyle.size() == 1) {
                    spansBuilder.add(Collections.singletonList(currentStyle.peek()), untilInd - lastInd);
                } else {
                    spansBuilder.add(new ArrayList<>(currentStyle), untilInd - lastInd);
                }
                lastInd = untilInd;
            } else if (untilInd == lastInd) {
                return;
            } else {
                throw new IllegalArgumentException("Cannot append empty style from " + lastInd + "-" + untilInd + " (must be ascending)");
            }
        }
        
        /**
         * 通用访问方法，为任意节点应用样式
         * @param node 要访问的节点
         * @param style 要应用的样式
         */
        private void visitAny(Node node, String style) {
            List<org.commonmark.node.SourceSpan> spans = node.getSourceSpans();
            if (spans.isEmpty()) {
                visitChildren(node);
                return;
            }
            
            org.commonmark.node.SourceSpan sourceFirst = spans.get(0);
            org.commonmark.node.SourceSpan sourceLast = spans.get(spans.size() - 1);
            
            int indStart = lineSums[sourceFirst.getLineIndex()] + sourceFirst.getColumnIndex();
            int indEnd = lineSums[sourceLast.getLineIndex()] + sourceLast.getColumnIndex() + sourceLast.getLength();
            
            appendStyle(indStart);
            currentStyle.push(style);
            visitChildren(node);
            appendStyle(indEnd);
            currentStyle.pop();
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            visitAny(blockQuote, HighlighterConstants.MD_QUOTE_STYLE);
        }

        @Override
        public void visit(BulletList bulletList) {
            visitAny(bulletList, HighlighterConstants.MD_LIST_STYLE);
        }

        @Override
        public void visit(Code code) {
            visitAny(code, HighlighterConstants.MD_CODE_STYLE);
        }

        @Override
        public void visit(Emphasis emphasis) {
            visitAny(emphasis, HighlighterConstants.MD_EMPHASIS_STYLE);
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            String language = fencedCodeBlock.getInfo();
            visitAny(fencedCodeBlock, HighlighterConstants.MD_CODE_STYLE);
        }


        @Override
        public void visit(Heading heading) {
            String headingStyle;
            switch (heading.getLevel()) {
                case 1: headingStyle = HighlighterConstants.MD_H1_STYLE; break;
                case 2: headingStyle = HighlighterConstants.MD_H2_STYLE; break;
                case 3: headingStyle = HighlighterConstants.MD_H3_STYLE; break;
                case 4: headingStyle = HighlighterConstants.MD_H4_STYLE; break;
                case 5: headingStyle = HighlighterConstants.MD_H5_STYLE; break;
                case 6: headingStyle = HighlighterConstants.MD_H6_STYLE; break;
                default: headingStyle = HighlighterConstants.MD_H6_STYLE;
            }
            visitAny(heading, headingStyle);
        }

        @Override
        public void visit(HtmlInline htmlInline) {
            visitAny(htmlInline, HighlighterConstants.MD_CODE_STYLE);
        }

        @Override
        public void visit(HtmlBlock htmlBlock) {
            visitAny(htmlBlock, HighlighterConstants.MD_CODE_STYLE);
        }

        @Override
        public void visit(Image image) {
            visitAny(image, HighlighterConstants.MD_IMAGE_STYLE);
        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {
            visitAny(indentedCodeBlock, HighlighterConstants.MD_CODE_STYLE);
        }

        @Override
        public void visit(Link link) {
            visitAny(link, HighlighterConstants.MD_LINK_STYLE);
        }

        @Override
        public void visit(OrderedList orderedList) {
            visitAny(orderedList, HighlighterConstants.MD_LIST_STYLE);
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            visitAny(strongEmphasis, HighlighterConstants.MD_STRONG_STYLE);
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {
            visitAny(thematicBreak, HighlighterConstants.MD_HR_STYLE);
        }

        // 其他节点类型的访问方法，只访问子节点不添加特殊样式
        @Override
        public void visit(LinkReferenceDefinition linkReferenceDefinition) {
            visitChildren(linkReferenceDefinition);
        }

        @Override
        public void visit(CustomBlock customBlock) {
            visitChildren(customBlock);
        }

        @Override
        public void visit(CustomNode customNode) {
            visitChildren(customNode);
        }
    }
}