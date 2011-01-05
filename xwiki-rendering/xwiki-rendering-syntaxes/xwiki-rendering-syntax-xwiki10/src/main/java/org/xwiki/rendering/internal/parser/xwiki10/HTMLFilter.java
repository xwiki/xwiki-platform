/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.internal.parser.xwiki10.html.InvalidHtmlException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.Filter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.HTMLElementConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Add needed HTML open and close macro.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("htmlmacro")
public class HTMLFilter extends AbstractFilter implements Initializable
{
    public static final String HTMLOPEN_SUFFIX = "htmlopen";

    public static final String HTMLCLOSE_SUFFIX = "htmlclose";

    public static final String HTMLOPEN_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENS_SF_SPATTERN + HTMLOPEN_SUFFIX + "\\d+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String HTMLCLOSE_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENS_SF_SPATTERN + HTMLCLOSE_SUFFIX + "\\d+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String EMPTYLINEVELOCITY_SPATTERN =
        "((?:" + FilterContext.XWIKI1020TOKENI_PATTERN + ")|[^" + FilterContext.XWIKI1020TOKEN_CP + "])\n("
            + VelocityFilter.NLGROUP_SPATTERN + ")\n" + "(" + FilterContext.XWIKI1020TOKENI_PATTERN + "|[^"
            + FilterContext.XWIKI1020TOKEN_OP + "]|$)";

    public static final Pattern EMPTYLINEVELOCITY_PATTERN = Pattern.compile(EMPTYLINEVELOCITY_SPATTERN);

    public static final Pattern LINEBREAK_PATTERN = Pattern.compile(Pattern.quote("\\\\"));

    /**
     * Used to lookup macros converters.
     */
    @Requirement
    private ComponentManager componentManager;

    @Requirement("escape20")
    private Filter escape20SyntaxFilter;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(3000);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.Filter#filter(java.lang.String,
     *      org.xwiki.rendering.parser.xwiki10.FilterContext)
     */
    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        char[] array = content.toCharArray();

        HTMLFilterContext context = new HTMLFilterContext(filterContext);

        StringBuffer beforeHtmlBuffer = new StringBuffer();
        StringBuffer htmlBuffer = new StringBuffer();
        StringBuffer afterHtmlBuffer = new StringBuffer();

        boolean inHTMLMacro = false;

        int i = 0;
        for (; i < array.length;) {
            char c = array[i];

            context.setConversion(false);
            context.pushType();
            context.setHTML(false);
            context.setVelocityOpen(false);
            context.setVelocityClose(false);
            context.setInline(true);

            StringBuffer nonHtmlbuffer = inHTMLMacro ? afterHtmlBuffer : beforeHtmlBuffer;

            if (c == '<') {
                try {
                    StringBuffer htmlBlock = new StringBuffer();

                    int start = i;
                    i = getHTMLBlock(array, i, null, htmlBlock, context);

                    StringBuffer buffer;
                    String str;
                    if (context.isHTML()) {
                        if (!inHTMLMacro) {
                            inHTMLMacro = true;
                        } else {
                            htmlBuffer.append(afterHtmlBuffer);
                            afterHtmlBuffer.setLength(0);
                        }

                        buffer = htmlBuffer;
                        str = context.cleanContent(new String(array, start, i - start));
                    } else {
                        buffer = nonHtmlbuffer;
                        str = htmlBlock.toString();
                    }

                    if (context.isVelocityOpen()) {
                        VelocityFilter.appendVelocityOpen(buffer, filterContext, false);
                    }

                    if (context.isConversion()) {
                        if (!context.isInline()) {
                            if (htmlBuffer.length() > 0 || buffer.length() > 0) {
                                CleanUtil.setTrailingNewLines(buffer, 2);
                            }
                        }
                    }

                    buffer.append(str);

                    if (context.isVelocityClose()) {
                        VelocityFilter.appendVelocityClose(buffer, filterContext, false);
                    }
                } catch (InvalidHtmlException e) {
                    getLogger().debug("Invalid HTML block at char [" + i + "]", e);

                    nonHtmlbuffer.append(c);
                    ++i;
                }
            } else {
                nonHtmlbuffer.append(c);
                ++i;
            }
        }

        String beforeHtmlContent = beforeHtmlBuffer.toString();
        String htmlContent = htmlBuffer.toString();
        String afterHtmlContent = afterHtmlBuffer.toString();

        // Include velocity content as HTML content since lot of velocity generates html
        if (htmlContent.length() > 0) {
            Matcher velocityBeforeMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(beforeHtmlBuffer);
            if (velocityBeforeMatcher.find()) {
                htmlContent = beforeHtmlContent.substring(velocityBeforeMatcher.start()) + htmlContent;
                beforeHtmlContent = beforeHtmlContent.substring(0, velocityBeforeMatcher.start());
            }
            Matcher velocityAfterMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(afterHtmlContent);
            if (velocityAfterMatcher.find()) {
                htmlContent = htmlContent + afterHtmlContent.substring(0, velocityAfterMatcher.end());
                afterHtmlContent = afterHtmlContent.substring(velocityAfterMatcher.end());
            }
        } else {
            Matcher velocityContentMatcher = VelocityFilter.VELOCITYCONTENT_PATTERN.matcher(beforeHtmlBuffer);

            if (velocityContentMatcher.find()) {
                htmlContent = velocityContentMatcher.group(0);
                afterHtmlContent = beforeHtmlContent.substring(velocityContentMatcher.end());
                beforeHtmlContent = beforeHtmlContent.substring(0, velocityContentMatcher.start());
            }
        }

        if (htmlContent.length() > 0) {
            boolean multilines = filterContext.unProtect(htmlContent).indexOf("\n") != -1;

            // Make sure html macro does not start in a block and ends in another by "eating" them
            if (multilines && htmlContent.indexOf("\n\n") != -1) {
                int beforeIndex = beforeHtmlContent.lastIndexOf("\n\n");

                if (beforeIndex == -1) {
                    htmlContent = beforeHtmlContent + htmlContent;
                    beforeHtmlContent = "";
                } else {
                    htmlContent = beforeHtmlContent.substring(beforeIndex + 2) + htmlContent;
                    beforeHtmlContent = beforeHtmlContent.substring(0, beforeIndex + 2);
                }

                int afterIndex = afterHtmlContent.indexOf("\n\n");

                if (afterIndex == -1) {
                    htmlContent += afterHtmlContent;
                    afterHtmlContent = "";
                } else {
                    htmlContent += afterHtmlContent.substring(0, afterIndex);
                    afterHtmlContent = afterHtmlContent.substring(afterIndex);
                }
            }

            // velocity open
            Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(htmlContent);
            boolean velocityOpen = velocityOpenMatcher.find();
            htmlContent = velocityOpenMatcher.replaceFirst("");

            // velocity close
            Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(htmlContent);
            boolean velocityClose = velocityCloseMatcher.find();
            htmlContent = velocityCloseMatcher.replaceFirst("");

            // Make sure empty lines are taken into account
            htmlContent = forceEmptyLines(htmlContent);
            // Make sure \\ line breaks are taken into account
            htmlContent = forceLineBreak(htmlContent);

            // Print

            // print before html
            result.append(beforeHtmlContent);

            // print html
            // open html content
            if (velocityOpen) {
                VelocityFilter.appendVelocityOpen(result, filterContext, multilines);
            }

            appendHTMLOpen(result, filterContext, multilines);

            // print html content
            result.append(filterContext.addProtectedContent(escape20SyntaxFilter.filter(htmlContent, filterContext),
                false));

            appendHTMLClose(result, filterContext, multilines);

            // close html content
            if (velocityClose) {
                VelocityFilter.appendVelocityClose(result, filterContext, multilines);
            }

            // print after html
            result.append(afterHtmlContent);
        } else {
            result = beforeHtmlBuffer;
        }

        return result.toString();
    }

    private String forceEmptyLines(String htmlContent)
    {
        return EMPTYLINEVELOCITY_PATTERN.matcher(htmlContent).replaceAll("$1\n$4<p/>\n$5");
    }

    private String forceLineBreak(String htmlContent)
    {
        return LINEBREAK_PATTERN.matcher(htmlContent).replaceAll("<br/>");
    }

    private int getHTMLBlock(char[] array, int currentIndex, StringBuffer elementName, StringBuffer htmlBlock,
        HTMLFilterContext context) throws InvalidHtmlException
    {
        int i = currentIndex + 1;

        context.setType(null);

        if (i < array.length) {
            if (array[i] == '/') {
                i = getEndElement(array, currentIndex, elementName, htmlBlock, context);
            } else if (array[i] == '!' && i + 2 < array.length && array[i + 1] == '-' && array[i + 2] == '-') {
                i = getComment(array, currentIndex, htmlBlock, context);
            } else {
                i = getElement(array, currentIndex, elementName, htmlBlock, context);
            }
        }

        return i;
    }

    private int getComment(char[] array, int currentIndex, StringBuffer commentBlock, HTMLFilterContext context)
    {
        context.setType(HTMLType.COMMENT);
        context.setHTML(true);

        int i = currentIndex + 4;

        for (; i < array.length && (array[i - 1] != '>' || array[i - 2] != '-' || array[i - 3] != '-'); ++i) {
        }

        commentBlock.append(array, currentIndex, i - currentIndex);

        return i;
    }

    private int getElement(char[] array, int currentIndex, StringBuffer elementNameBuffer, StringBuffer element,
        HTMLFilterContext context) throws InvalidHtmlException
    {
        // If white space it's not html
        if (Character.isWhitespace(array[currentIndex + 1])) {
            throw new InvalidHtmlException();
        }

        // get begin element
        StringBuffer beginElement = new StringBuffer();
        Map<String, String> parameterMap = new LinkedHashMap<String, String>();
        if (elementNameBuffer == null) {
            elementNameBuffer = new StringBuffer();
        }
        int i = getBeginElement(array, currentIndex, elementNameBuffer, beginElement, parameterMap, context);

        String elementName = elementNameBuffer.toString();

        // force <br> as full element instead of just begin element
        if (context.peekType() == HTMLType.BEGIN && "br".equals(elementName)) {
            context.setType(HTMLType.ELEMENT);
        }

        // Get content
        StringBuffer elementContent = null;
        if (context.peekType() == HTMLType.BEGIN) {
            elementContent = new StringBuffer();
            context.pushType();
            i = getElementContent(array, i, elementName, elementContent, null, context);
            context.popType();
        }

        // Convert
        String convertedElement =
            convertElement(elementName, elementContent != null ? elementContent.toString() : null, parameterMap,
                context);

        // Print
        if (convertedElement != null) {
            element.append(convertedElement);
        } else {
            context.setHTML(true);
        }

        context.setType(HTMLType.ELEMENT);

        return i;
    }

    private String convertElement(String name, String content, Map<String, String> parameters, HTMLFilterContext context)
    {
        String convertedElement = null;

        context.setConversion(false);

        try {
            HTMLElementConverter currentMacro = this.componentManager.lookup(HTMLElementConverter.class, name);

            convertedElement = currentMacro.convert(name, parameters, content, context);

            if (convertedElement != null) {
                context.setConversion(true);
                context.setInline(currentMacro.isInline());
            }
        } catch (ComponentLookupException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Can't find macro converter [" + name + "]", e);
            }
        } catch (Exception e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Failed to convert macro [" + name + "]", e);
            }
        }

        return convertedElement;
    }

    private int getElementContent(char[] array, int currentIndex, String currentElement, StringBuffer elementContent,
        StringBuffer endElement, HTMLFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length;) {
            char c = array[i];

            context.setConversion(false);

            StringBuffer htmlBlock = new StringBuffer();

            String elementName;
            if (c == '<') {
                try {
                    StringBuffer elementNameBuffer = new StringBuffer();
                    i = getHTMLBlock(array, i, elementNameBuffer, htmlBlock, context);
                    elementName = elementNameBuffer.toString();

                    if (context.peekType() == HTMLType.END
                        && (currentElement.equals(elementName) || currentElement.startsWith(elementName))) {
                        if (endElement != null) {
                            endElement.append(htmlBlock);
                        }
                        break;
                    }

                    if (context.peekType() != null) {
                        elementContent.append(htmlBlock);
                    } else {
                        elementContent.append(c);
                        ++i;
                    }
                } catch (InvalidHtmlException e) {
                    getLogger().debug("Invalid HTML block at char [" + i + "]", e);
                    ++i;
                }
            } else {
                elementContent.append(c);
                ++i;
            }
        }

        return i;
    }

    private int getBeginElement(char[] array, int currentIndex, StringBuffer elementName, StringBuffer beginElement,
        Map<String, String> parameterMap, HTMLFilterContext context) throws InvalidHtmlException
    {
        int i = currentIndex + 1;

        // If white space it's not html
        if (i == array.length || Character.isWhitespace(array[i])) {
            throw new InvalidHtmlException();
        }

        // get element name
        i = getElementName(array, i, elementName, context);

        // skip white spaces
        i = getWhiteSpaces(array, i, null);

        // get parameters
        i = getElementParameters(array, i, null, parameterMap);

        // skip white spaces
        i = getWhiteSpaces(array, i, null);

        // 
        if (array[i] == '/' && i + 1 < array.length && array[i + 1] == '>') {
            context.setType(HTMLType.ELEMENT);
            i += 2;
        } else {
            context.setType(HTMLType.BEGIN);
            i += 1;
        }

        beginElement.append(array, currentIndex, i - currentIndex);

        return i;
    }

    private int getEndElement(char[] array, int currentIndex, StringBuffer elementName, StringBuffer endElement,
        HTMLFilterContext context) throws InvalidHtmlException
    {
        int i = currentIndex + 2;

        // If white space it's not html
        if (i == array.length || Character.isWhitespace(array[i])) {
            throw new InvalidHtmlException();
        }

        // get element name
        i = getElementName(array, i, elementName, context);

        // skip white spaces
        i = getWhiteSpaces(array, i, null);

        if (array[i] == '>') {
            ++i;
        }

        context.setType(HTMLType.END);

        if (endElement != null) {
            endElement.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getElementName(char[] array, int currentIndex, StringBuffer elementName, HTMLFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length && !Character.isWhitespace(array[i]); ++i) {
            if (array[i] == '>') {
                context.setType(HTMLType.BEGIN);
                break;
            } else if (array[i] == '/' && i + 1 < array.length && array[i + 1] == '>') {
                context.setType(HTMLType.END);
                break;
            }
        }

        if (elementName != null) {
            elementName.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getWhiteSpaces(char[] array, int currentIndex, StringBuffer htmlBlock)
    {
        int i = currentIndex;

        for (; i < array.length && Character.isWhitespace(array[i]); ++i) {
        }

        if (htmlBlock != null) {
            htmlBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getElementParameters(char[] array, int currentIndex, StringBuffer parametersBlock,
        Map<String, String> parameterMap)
    {
        int i = currentIndex;

        for (; i < array.length;) {
            // Skip white spaces
            i = getWhiteSpaces(array, i, null);

            if (i < array.length) {
                // If '>' it's the end of parameters
                if (array[i] == '>') {
                    break;
                } else if (array[i] == '/' && i + 1 < array.length && array[i + 1] == '>') {
                    break;
                }

                // Skip parameter
                i = getElementParameter(array, i, null, parameterMap);
            }
        }

        if (parametersBlock != null) {
            parametersBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getElementParameter(char[] array, int currentIndex, StringBuffer parameterBlock,
        Map<String, String> parameterMap)
    {
        int i = currentIndex;

        // Get key
        StringBuffer keyBlock = new StringBuffer();
        for (; i < array.length && array[i] != '>' && array[i] != '=' && !Character.isWhitespace(array[i]); ++i) {
            keyBlock.append(array[i]);
        }

        // Skip white spaces
        i = getWhiteSpaces(array, i, null);

        // Equal sign
        if (array[i] == '=') {
            ++i;

            // Skip white spaces
            i = getWhiteSpaces(array, i, null);

            // Get value
            StringBuffer valueBlock = new StringBuffer();
            i = getElementParameterValue(array, i, valueBlock);

            // Add a new parameter to the list
            parameterMap.put(keyBlock.toString(), valueBlock.toString());
        }

        if (parameterBlock != null) {
            parameterBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getElementParameterValue(char[] array, int currentIndex, StringBuffer valueBlock)
    {
        int i = currentIndex;

        char escaped = 0;

        if (array[i] == '"' || array[i] == '\'') {
            escaped = array[i];
            ++i;
        }

        for (; i < array.length && array[i] != '>'; ++i) {
            if (escaped == 0) {
                if (Character.isWhitespace(array[i])) {
                    break;
                }
            } else {
                if (array[i] == escaped) {
                    ++i;
                    break;
                }
            }
        }

        valueBlock.append(array, currentIndex, i - currentIndex);

        return i;
    }

    public static void appendHTMLOpen(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent("{{html clean=\"false\" wiki=\"true\"}}" + (nl ? "\n" : ""),
            HTMLOPEN_SUFFIX, true));
    }

    public static void appendHTMLClose(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent((nl ? "\n" : "") + "{{/html}}", HTMLCLOSE_SUFFIX, true));
    }

    public static class HTMLFilterContext
    {
        private boolean conversion = false;

        private Stack<HTMLType> type = new Stack<HTMLType>();

        private boolean html = false;

        private FilterContext filterContext;

        private boolean velocityOpen;

        private boolean velocityClose;

        private boolean inline;

        public HTMLFilterContext(FilterContext filterContext)
        {
            this.filterContext = filterContext;
        }

        public boolean isConversion()
        {
            return this.conversion;
        }

        public void setConversion(boolean conversion)
        {
            this.conversion = conversion;
        }

        public FilterContext getFilterContext()
        {
            return this.filterContext;
        }

        public HTMLType peekType()
        {
            return type.peek();
        }

        public void pushType()
        {
            this.type.push(null);
        }

        public HTMLType popType()
        {
            return this.type.pop();
        }

        public void setType(HTMLType type)
        {
            this.type.set(this.type.size() - 1, type);
        }

        public boolean isHTML()
        {
            return html;
        }

        public void setHTML(boolean isHTML)
        {
            this.html = isHTML;
        }

        public boolean isVelocityOpen()
        {
            return velocityOpen;
        }

        public void setVelocityOpen(boolean velocityOpen)
        {
            this.velocityOpen = velocityOpen;
        }

        public boolean isVelocityClose()
        {
            return velocityClose;
        }

        public void setVelocityClose(boolean velocityClose)
        {
            this.velocityClose = velocityClose;
        }

        public boolean isInline()
        {
            return this.inline;
        }

        public void setInline(boolean inline)
        {
            this.inline = inline;
        }

        public String cleanContent(String content)
        {
            Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(content);
            setVelocityOpen(isVelocityOpen() | velocityOpenMatcher.find());
            String cleanedContent = velocityOpenMatcher.replaceFirst("");

            Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedContent);
            setVelocityClose(isVelocityClose() | velocityCloseMatcher.find());
            cleanedContent = velocityCloseMatcher.replaceFirst("");

            return cleanedContent;
        }
    }

    enum HTMLType
    {
        ELEMENT,
        BEGIN,
        END,
        COMMENT
    }
}
