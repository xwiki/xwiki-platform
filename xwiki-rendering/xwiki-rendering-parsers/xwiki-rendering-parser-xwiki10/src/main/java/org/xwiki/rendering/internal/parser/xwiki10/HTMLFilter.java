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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.HTMLElementConverter;

/**
 * Add needed HTML open and close macro.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("htmlmacro")
public class HTMLFilter extends AbstractFilter implements Initializable, Composable
{
    public static final String HTML_SPATTERN = "(\\<!--)|(--\\>)|([\\<\\>])";

    public static final Pattern HTML_PATTERN = Pattern.compile(HTML_SPATTERN);

    public static final String HTMLOPEN_SUFFIX = "htmlopen";

    public static final String HTMLCLOSE_SUFFIX = "htmlclose";

    public static final String HTMLOPEN_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENNI + HTMLOPEN_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String HTMLCLOSE_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENNI + HTMLCLOSE_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    /**
     * Used to lookup macros converters.
     */
    private ComponentManager componentManager;

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
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
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
        StringBuffer nonHTMLContent = new StringBuffer();
        StringBuffer htmlContent = new StringBuffer();

        boolean inHTMLMacro = false;

        int i = 0;
        for (; i < array.length;) {
            char c = array[i];

            context.setConversion(false);
            context.setElementName(null);
            context.setType(null);
            context.setVelocityOpen(false);
            context.setVelocityClose(false);

            StringBuffer htmlBlock = new StringBuffer();

            if (c == '<') {
                i = getHTMLBlock(array, i, htmlBlock, context);
            }

            if (context.isConversion()) {
                StringBuffer nonHtmlbuffer = inHTMLMacro ? nonHTMLContent : result;

                if (context.isVelocityOpen()) {
                    VelocityFilter.appendVelocityOpen(nonHtmlbuffer, filterContext, false);
                }

                nonHtmlbuffer.append(htmlBlock);

                if (context.isVelocityClose()) {
                    VelocityFilter.appendVelocityClose(nonHtmlbuffer, filterContext, false);
                }
            } else if (context.getType() != null) {
                if (!inHTMLMacro) {
                    inHTMLMacro = true;
                } else {
                    htmlContent.append(nonHTMLContent);
                    nonHTMLContent.setLength(0);
                }

                htmlContent.append(htmlBlock);
            } else {
                if (!inHTMLMacro) {
                    result.append(c);
                } else {
                    nonHTMLContent.append(c);
                }
                ++i;
            }
        }

        // remove velocity macro marker from html content
        String cleanedHtmlContent = htmlContent.toString();

        // velocity open
        Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(result);
        boolean velocityOpenBefore = velocityOpenMatcher.find();

        boolean velocityOpen = false;
        if (!velocityOpenBefore) {
            velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(cleanedHtmlContent);
            velocityOpen = velocityOpenMatcher.find();
            cleanedHtmlContent = velocityOpenMatcher.replaceFirst("");
        }

        // velocity close
        Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(result);
        boolean velocityCloseBefore = velocityCloseMatcher.find();

        boolean velocityClose = false;
        if (!velocityCloseBefore) {
            velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedHtmlContent);
            velocityClose = velocityCloseMatcher.find();
            cleanedHtmlContent = velocityCloseMatcher.replaceFirst("");
        }

        if (cleanedHtmlContent.length() > 0) {
            boolean multilines = cleanedHtmlContent.indexOf("\n") != -1;

            // open html content
            if (velocityOpen) {
                VelocityFilter.appendVelocityOpen(result, filterContext, multilines);
            } else if (!velocityOpenBefore || velocityCloseBefore) {
                appendHTMLOpen(result, filterContext, multilines);
            }

            // print html content
            result.append(cleanedHtmlContent);

            // close html content
            if (velocityClose) {
                VelocityFilter.appendVelocityClose(result, filterContext, multilines);
            } else if (!velocityOpenBefore || velocityCloseBefore) {
                appendHTMLClose(result, filterContext, multilines);
            }
        }

        // print end
        result.append(nonHTMLContent);

        return result.toString();
    }

    public int getHTMLBlock(char[] array, int currentIndex, StringBuffer htmlBlock, HTMLFilterContext context)
    {
        int i = currentIndex + 1;

        if (i < array.length) {
            if (array[i] == '/') {
                i = getEndElement(array, currentIndex, htmlBlock, context);
            } else if (array[i] == '!' && i + 2 < array.length && array[i + 1] == '-' && array[i + 2] == '-') {
                i = getComment(array, currentIndex, htmlBlock, context);
            } else {
                i = getElement(array, currentIndex, htmlBlock, context);
            }
        }

        return i;
    }

    public int getComment(char[] array, int currentIndex, StringBuffer htmlBlock, HTMLFilterContext context)
    {
        context.setType(HTMLType.COMMENT);

        StringBuffer commentBlock = new StringBuffer();

        commentBlock.append("<!--");

        int i = currentIndex + 4;

        for (; i < array.length; ++i) {
            if (array[i] == '-' && i + 2 < array.length && array[i + 1] == '-' && array[i + 2] == '>') {
                commentBlock.append("-->");
                i += 3;
                break;
            }
            commentBlock.append(array[i]);
        }

        htmlBlock.append(context.addProtectedContent(commentBlock.toString(), true));

        return i;
    }

    public int getElement(char[] array, int currentIndex, StringBuffer element, HTMLFilterContext context)
    {
        // If white space it's not html
        if (Character.isWhitespace(array[currentIndex + 1])) {
            context.setType(null);
            return currentIndex;
        }

        // get begin element
        StringBuffer beginElement = new StringBuffer();
        Map<String, String> parameterMap = new LinkedHashMap<String, String>();
        int i = getBeginElement(array, currentIndex, beginElement, parameterMap, context);

        String elementName = context.getElementName();

        // force <br> as full element instead of just begin element
        if (context.getType() == HTMLType.BEGIN && elementName.equals("br")) {
            context.setType(HTMLType.ELEMENT);
        }

        // Get content
        StringBuffer elementContent = null;
        if (context.getType() == HTMLType.BEGIN) {
            elementContent = new StringBuffer();
            i = getElementContent(array, i, elementContent, context);
        }

        // Convert
        String convertedElement =
            convertElement(elementName, elementContent != null ? elementContent.toString() : null, parameterMap,
                context);

        // Print
        if (convertedElement != null) {
            element.append(convertedElement);
        } else {
            element.append(beginElement);
            if (elementContent != null) {
                element.append(elementContent);
                if (context.getType() == HTMLType.END) {
                    element.append(context.getFilterContext().addProtectedContent("</" + elementName + '>', false));
                }
            }

            context.setType(HTMLType.ELEMENT);
        }

        return i;
    }

    private String convertElement(String name, String content, Map<String, String> parameters, HTMLFilterContext context)
    {
        String convertedElement = null;

        context.setConversion(false);

        try {
            HTMLElementConverter currentMacro =
                (HTMLElementConverter) this.componentManager.lookup(HTMLElementConverter.class.getName(), name);

            convertedElement = currentMacro.convert(name, parameters, content, context);

            context.setConversion(true);
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

    public int getElementContent(char[] array, int currentIndex, StringBuffer elementContent, HTMLFilterContext context)
    {
        int i = currentIndex;

        String currentElementName = context.getElementName();

        for (; i < array.length;) {
            char c = array[i];

            context.setConversion(false);
            context.setElementName(null);
            context.setType(null);

            StringBuffer htmlBlock = new StringBuffer();

            if (c == '<') {
                i = getHTMLBlock(array, i, htmlBlock, context);
            }

            if (context.getType() == HTMLType.END && currentElementName.equals(context.getElementName())) {
                break;
            }

            if (context.isConversion()) {
                elementContent.append(htmlBlock);
            } else if (context.getType() != null) {
                elementContent.append(htmlBlock);
            } else {
                elementContent.append(c);
                ++i;
            }
        }

        return i;
    }

    public int getBeginElement(char[] array, int currentIndex, StringBuffer beginElement,
        Map<String, String> parameterMap, HTMLFilterContext context)
    {
        beginElement.append("<");

        int i = currentIndex + 1;

        // get element name
        StringBuffer elementName = new StringBuffer();
        i = getElementName(array, i, elementName, context);
        context.setElementName(elementName.toString());
        beginElement.append(elementName);

        // skip white spaces
        i = getWhiteSpaces(array, i, beginElement, context);

        // 
        if (array[i] == '/' && i + 1 < array.length && array[i + 1] == '>') {
            context.setType(HTMLType.ELEMENT);
            beginElement.append("/>");
            return i + 2;
        }

        // get parameters
        i = getElementParameters(array, i, beginElement, parameterMap, context);

        // skip white spaces
        i = getWhiteSpaces(array, i, beginElement, context);

        beginElement.append(">");

        if (array[i] == '>') {
            ++i;
        }

        context.setType(HTMLType.BEGIN);

        return i;
    }

    public int getEndElement(char[] array, int currentIndex, StringBuffer endElement, HTMLFilterContext context)
    {
        endElement.append("</");

        int i = currentIndex + 2;

        // If white space it's not html
        if (i == array.length || Character.isWhitespace(array[i])) {
            context.setType(null);
            return currentIndex;
        }

        // get element name
        StringBuffer elementName = new StringBuffer();
        i = getElementName(array, i, elementName, context);
        context.setElementName(elementName.toString());
        endElement.append(elementName);

        // skip white spaces
        i = getWhiteSpaces(array, i, endElement, context);

        endElement.append(">");

        if (array[i] == '>') {
            ++i;
        }

        context.setType(HTMLType.END);

        return i;
    }

    public int getElementName(char[] array, int currentIndex, StringBuffer elementName, HTMLFilterContext context)
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

            elementName.append(array[i]);
        }

        return i;
    }

    public int getWhiteSpaces(char[] array, int currentIndex, StringBuffer result, HTMLFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length && Character.isWhitespace(array[i]); ++i) {
            result.append(array[i]);
        }

        return i;
    }

    private int getElementParameters(char[] array, int currentIndex, StringBuffer parametersBlock,
        Map<String, String> parameterMap, HTMLFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length;) {
            // Skip white spaces
            i = getWhiteSpaces(array, i, parametersBlock, context);

            if (i < array.length) {
                // If '>' it's the end of parameters
                if (array[i] == '>') {
                    break;
                }

                // Skip parameter
                i = getElementParameter(array, i, parametersBlock, parameterMap, context);
            }
        }

        return i;
    }

    private int getElementParameter(char[] array, int currentIndex, StringBuffer parameterBlock,
        Map<String, String> parameterMap, HTMLFilterContext context)
    {
        int i = currentIndex;

        // Get key
        StringBuffer keyBlock = new StringBuffer();
        for (; i < array.length && array[i] != '>' && array[i] != '=' && !Character.isWhitespace(array[i]); ++i) {
            keyBlock.append(array[i]);
        }
        parameterBlock.append(keyBlock);

        // Skip white spaces
        i = getWhiteSpaces(array, i, parameterBlock, context);

        // Equal sign
        if (array[i] == '=') {
            ++i;

            // Skip white spaces
            i = getWhiteSpaces(array, i, parameterBlock, context);

            // Get value
            StringBuffer valueBlock = new StringBuffer();
            for (; i < array.length && array[i] != '>' && !Character.isWhitespace(array[i]); ++i) {
                valueBlock.append(array[i]);
            }
            parameterBlock.append(valueBlock);

            parameterMap.put(keyBlock.toString(), valueBlock.toString());
        }

        return i;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.Filter#filter(java.lang.String,
     *      org.xwiki.rendering.parser.xwiki10.FilterContext)
     */
    public String filter2(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = HTML_PATTERN.matcher(content);

        boolean inHTMLMacro = false;
        boolean inHTMLComment = false;

        boolean velocityOpenBefore = false;
        boolean velocityCloseBefore = false;

        StringBuffer htmlContent = new StringBuffer();

        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (!inHTMLMacro) {
                Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(before);
                velocityOpenBefore = velocityOpenMatcher.find();
                Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(before);
                velocityCloseBefore = velocityCloseMatcher.find();

                result.append(before);
            } else {
                htmlContent.append(before);
            }

            inHTMLMacro = true;

            if (matcher.group(1) != null) {
                inHTMLComment = true;
                htmlContent.append(filterContext.addProtectedContent(matcher.group(0)));
            } else if (inHTMLComment && matcher.group(2) != null) {
                htmlContent.append(filterContext.addProtectedContent(matcher.group(0)));
                inHTMLComment = false;
            } else {
                htmlContent.append(matcher.group(0));
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        // clean html content
        Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(htmlContent);
        boolean velocityOpen = velocityOpenMatcher.find();
        String cleanedHtmlContent = velocityOpenMatcher.replaceAll("");
        Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedHtmlContent);
        boolean velocityClose = velocityCloseMatcher.find();
        cleanedHtmlContent = velocityCloseMatcher.replaceAll("");

        // print the content

        boolean multilines = cleanedHtmlContent.indexOf("\n") != -1;

        if (velocityOpen) {
            VelocityFilter.appendVelocityOpen(result, filterContext, multilines);
        } else if (!velocityOpenBefore || velocityCloseBefore) {
            appendHTMLOpen(result, filterContext, multilines);
        }

        result.append(cleanedHtmlContent);

        if (velocityClose) {
            VelocityFilter.appendVelocityClose(result, filterContext, multilines);
        } else if (velocityCloseBefore || !velocityOpenBefore) {
            appendHTMLClose(result, filterContext, multilines);
        }

        if (currentIndex < content.length()) {
            result.append(content.substring(currentIndex));
        }

        return result.toString();
    }

    public static void appendHTMLOpen(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext
            .addProtectedContent("{{html wiki=true}}" + (nl ? "\n" : ""), HTMLOPEN_SUFFIX, false));
    }

    public static void appendHTMLClose(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent((nl ? "\n" : "") + "{{/html}}", HTMLCLOSE_SUFFIX, false));
    }

    public static class HTMLFilterContext
    {
        private boolean conversion = false;

        private HTMLType type = null;

        private FilterContext filterContext;

        private String elementName;

        private boolean velocityOpen;

        private boolean velocityClose;

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

        public HTMLType getType()
        {
            return type;
        }

        public void setType(HTMLType type)
        {
            this.type = type;
        }

        public String getElementName()
        {
            return elementName;
        }

        public void setElementName(String elementName)
        {
            this.elementName = elementName;
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

        public String cleanContent(String content)
        {
            String cleanedContent = content;
            if (!isVelocityOpen()) {
                Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(content);
                setVelocityOpen(isVelocityOpen() | velocityOpenMatcher.find());
                cleanedContent = velocityOpenMatcher.replaceFirst("");
            }

            if (!isVelocityClose()) {
                Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedContent);
                setVelocityClose(isVelocityClose() | velocityCloseMatcher.find());
                cleanedContent = velocityCloseMatcher.replaceFirst("");
            }

            return cleanedContent;
        }

        public String addProtectedContent(String content, boolean inline)
        {
            return getFilterContext().addProtectedContent(cleanContent(content), inline);
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
