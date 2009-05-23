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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Register all Velocity comments in order to protect them from following filters. Protect velocity comments, convert
 * velocity macro into 2.0 macros/syntax and add needed 2.0 velocity macros and convert.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("velocity")
public class VelocityFilter extends AbstractFilter implements Composable, Initializable
{
    public static final String VELOCITYOPEN_SUFFIX = "velocityopen";

    public static final String VELOCITYCLOSE_SUFFIX = "velocityclose";

    public static final Set<String> VELOCITY_BEGINBLOCK = new HashSet<String>(Arrays.asList("if", "foreach"));

    public static final Set<String> VELOCITY_PARAMBLOCK = new HashSet<String>(Arrays.asList("if", "foreach", "set"));

    public static final Set<String> VELOCITY_NOPARAMBLOCK = new HashSet<String>(Arrays.asList("else"));

    public static final Set<String> VELOCITY_ENDBLOCK = new HashSet<String>(Arrays.asList("end"));

    public static final String VELOCITYOPEN_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VelocityFilter.VELOCITYOPEN_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYCLOSE_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VelocityFilter.VELOCITYCLOSE_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final Pattern VELOCITYOPEN_PATTERN = Pattern.compile(VelocityFilter.VELOCITYOPEN_SPATTERN);

    public static final Pattern VELOCITYCLOSE_PATTERN = Pattern.compile(VelocityFilter.VELOCITYCLOSE_SPATTERN);

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
        setPriority(20);
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

        VelocityFilterContext context = new VelocityFilterContext(filterContext);

        StringBuffer beforeVelocityBuffer = new StringBuffer();
        StringBuffer velocityBuffer = new StringBuffer();
        StringBuffer afterVelocityBuffer = new StringBuffer();

        boolean inVelocityMacro = false;
        int i = 0;

        for (; i < array.length;) {
            char c = array[i];

            context.setVelocity(false);
            context.setConversion(false);
            context.setInline(true);
            context.setProtectedBlock(true);

            StringBuffer velocityBlock = new StringBuffer();

            if (c == '#') {
                i = getKeyWord(array, i, velocityBlock, context);
            } else if (c == '$') {
                i = getVar(array, i, velocityBlock, context);
            }

            if (context.isVelocity()) {
                if (!inVelocityMacro) {
                    inVelocityMacro = true;
                } else {
                    velocityBuffer.append(afterVelocityBuffer);
                    afterVelocityBuffer.setLength(0);
                }

                if (context.isConversion()) {
                    if (!context.isInline()) {
                        if (velocityBuffer.length() > 0) {
                            CleanUtil.setTrailingNewLines(velocityBuffer, 2);
                        }
                    }
                }

                velocityBuffer.append(context.isProtectedBlock() ? filterContext.addProtectedContent(velocityBlock
                    .toString(), context.isInline()) : velocityBlock);
            } else {
                StringBuffer nonVelocityBuffer = inVelocityMacro ? afterVelocityBuffer : beforeVelocityBuffer;

                if (context.isConversion()) {
                    if (!context.isInline()) {
                        if (nonVelocityBuffer.length() > 0 || velocityBuffer.length() > 0) {
                            CleanUtil.setTrailingNewLines(nonVelocityBuffer, 2);
                        }
                    }

                    nonVelocityBuffer.append(context.isProtectedBlock() ? filterContext.addProtectedContent(
                        velocityBlock.toString(), context.isInline()) : velocityBlock);
                } else {
                    nonVelocityBuffer.append(c);
                    ++i;
                }
            }
        }

        // fix not closed #if, #foreach
        if (context.getVelocityDepth() > 0) {
            velocityBuffer.append(afterVelocityBuffer);
            afterVelocityBuffer.setLength(0);

            // fix unclosed velocity blocks
            for (; context.getVelocityDepth() > 0; context.popVelocityDepth()) {
                velocityBuffer.append(filterContext.addProtectedContent("#end"));
            }
        }

        if (velocityBuffer.length() > 0) {
            String beforeVelocityContent = beforeVelocityBuffer.toString();
            String velocityContent = velocityBuffer.toString();
            String afterVelocityContent = afterVelocityBuffer.toString();

            boolean multilines = velocityBuffer.indexOf("\n") != -1;

            // Make sure velocity macro does not start in a block and ends in another by "eating" them
            if (multilines && velocityContent.indexOf("\n\n") != -1) {
                int beforeIndex = beforeVelocityContent.lastIndexOf("\n\n");

                if (beforeIndex == -1) {
                    velocityContent = beforeVelocityContent + velocityContent;
                    beforeVelocityContent = "";
                } else {
                    velocityContent = beforeVelocityContent.substring(beforeIndex + 2) + velocityContent;
                    beforeVelocityContent = beforeVelocityContent.substring(0, beforeIndex + 2);
                }

                int afterIndex = afterVelocityContent.lastIndexOf("\n\n");

                if (afterIndex == -1) {
                    velocityContent += afterVelocityContent;
                    afterVelocityContent = "";
                } else {
                    velocityContent += afterVelocityContent.substring(0, afterIndex);
                    afterVelocityContent = afterVelocityContent.substring(afterIndex);
                }
            }

            // print before velocity content
            result.append(beforeVelocityContent);

            // print velocity content
            appendVelocityOpen(result, filterContext, multilines);
            result.append(velocityContent);
            appendVelocityClose(result, filterContext, multilines);

            // print after velocity content
            result.append(afterVelocityContent);
        } else {
            result = beforeVelocityBuffer;
        }

        return result.toString();
    }

    private int getKeyWord(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
    {
        int i = currentIndex + 1;

        if (i >= array.length) {
            context.setVelocity(false);
            return i;
        }

        context.setInline(false);

        if (array[i] == '#') {
            // A simple line comment
            i = getSimpleComment(array, i, velocityBlock, context);
        } else if (array[i] == '*') {
            // A multi lines comment
            i = getMultilinesComment(array, i, velocityBlock, context);
        } else if (Character.isLetter(array[i])) {
            // A macro
            i = getMacro(array, i, velocityBlock, context);
        } else {
            i = currentIndex;
        }

        return i;
    }

    private int getMacro(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
    {
        StringBuffer macroBlock = new StringBuffer("#");

        int i = currentIndex;

        // Get macro name
        StringBuffer macroName = new StringBuffer();
        for (; i < array.length && Character.isLetterOrDigit(array[i]); ++i) {
            macroName.append(array[i]);
        }
        macroBlock.append(macroName);

        if (VELOCITY_ENDBLOCK.contains(macroName.toString())) {
            // #end
            context.popVelocityDepth();
            context.setVelocity(true);
        } else if (VELOCITY_NOPARAMBLOCK.contains(macroName.toString())) {
            context.setVelocity(true);
        } else {
            if (VELOCITY_BEGINBLOCK.contains(macroName.toString())) {
                // #if, #foreach
                context.pushVelocityDepth();
                context.setVelocity(true);
            }

            // Skip spaces
            for (; i < array.length && array[i] == ' '; ++i) {
                macroBlock.append(array[i]);
            }

            if (i < array.length) {
                if (array[i] == '(') {
                    if (VELOCITY_PARAMBLOCK.contains(macroName.toString())) {
                        // Skip condition
                        i = getMethodParameters(array, i, macroBlock, context);

                        context.setVelocity(true);
                    } else {
                        context.setInline(true);

                        List<String> parameters = new ArrayList<String>();
                        // Get condition
                        i = getMacroParameters(array, i, macroBlock, parameters, context);
                        String convertedMacro = convertMacro(macroName.toString(), parameters, context);

                        if (convertedMacro != null) {
                            // Apply conversion
                            macroBlock.setLength(0);
                            macroBlock.append(convertedMacro);
                        } else {
                            context.setVelocity(true);
                        }
                    }
                }
            }
        }

        velocityBlock.append(macroBlock);

        return i;
    }

    private String convertMacro(String name, List<String> parameters, VelocityFilterContext context)
    {
        String convertedMacro = null;

        try {
            VelocityMacroConverter currentMacro =
                (VelocityMacroConverter) this.componentManager.lookup(VelocityMacroConverter.class, name);

            convertedMacro = currentMacro.convert(name, parameters, context.getFilterContext());

            context.setInline(currentMacro.isInline());
            context.setProtectedBlock(currentMacro.protectResult());

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

        return convertedMacro;
    }

    private int getMacroParameters(char[] array, int currentIndex, StringBuffer velocityBlock,
        List<String> parameterList, VelocityFilterContext context)
    {
        velocityBlock.append('(');

        int i = currentIndex + 1;

        boolean isVelocity = false;

        for (; i < array.length;) {
            i = getMacroParametersSeparator(array, i, velocityBlock, context);

            if (i < array.length) {
                // If ')' it's the end of parameters
                if (array[i] == ')') {
                    velocityBlock.append(')');
                    ++i;
                    break;
                }

                // Skip parameter
                StringBuffer parameterBlock = new StringBuffer();
                i = getMacroParameter(array, i, parameterBlock, context);
                isVelocity |= context.isVelocity();
                parameterList.add(parameterBlock.toString());

                velocityBlock.append(parameterBlock);
            }
        }

        context.setVelocity(isVelocity);

        return i;
    }

    private int getMacroParametersSeparator(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        int i = currentIndex;

        i = getWhiteSpaces(array, i, velocityBlock, context);
        if (array[i] == ',') {
            velocityBlock.append(array[i++]);
        }
        i = getWhiteSpaces(array, i, velocityBlock, context);

        return i;
    }

    private int getMacroParameter(char[] array, int currentIndex, StringBuffer parameterBlock,
        VelocityFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length; ++i) {
            if (array[i] == '$') {
                i = getVar(array, i, parameterBlock, context);
                if (context.isVelocity()) {
                    break;
                }
            } else if (array[i] == '"') {
                i = getEscape(array, i, parameterBlock, '"', context);
                break;
            } else if (array[i] == '\'') {
                i = getEscape(array, i, parameterBlock, '\'', context);
                break;
            } else if (Character.isWhitespace(array[i]) || array[i] == ',') {
                break;
            } else if (array[i] == ')') {
                break;
            }

            parameterBlock.append(array[i]);
        }

        return i;
    }

    private int getSimpleComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        context.setVelocity(true);

        velocityBlock.append("##");

        int i = currentIndex + 1;

        for (; i < array.length; ++i) {
            if (array[i] == '\n') {
                break;
            }

            velocityBlock.append(array[i]);
        }

        return i;
    }

    private int getMultilinesComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        context.setVelocity(true);

        velocityBlock.append("#*");

        int i = currentIndex + 1;

        for (; i < array.length; ++i) {
            if (array[i] == '#' && array[i - 1] == '*') {
                velocityBlock.append('#');
                ++i;
                break;
            }

            velocityBlock.append(array[i]);
        }

        return i;
    }

    private int getVar(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
    {
        StringBuffer varBlock = new StringBuffer("$");

        int i = currentIndex + 1;

        if (i < array.length) {
            if (array[i] == '!') {
                varBlock.append('!');
                ++i;
            }

            if (i < array.length) {
                boolean fullSyntax = false;
                if (array[i] == '{') {
                    varBlock.append('{');
                    ++i;
                    fullSyntax = true;
                }

                if (i < array.length) {
                    // A Velocity variable starts with [a-zA-Z]
                    if (Character.isLetter(array[i])) {
                        context.setVelocity(true);

                        // Skip variable
                        for (; i < array.length && Character.isLetterOrDigit(array[i]); ++i) {
                            varBlock.append(array[i]);
                        }

                        // Skip method(s)
                        for (; i < array.length;) {
                            if (fullSyntax && array[i] == '}') {
                                varBlock.append('}');
                                ++i;
                                break;
                            } else if (array[i] == '.') {
                                i = getMethod(array, i, varBlock, context);
                                if (!context.isVelocity()) {
                                    break;
                                }
                            } else if (array[i] == '[') {
                                i = getTableElement(array, i, varBlock, context);
                                break;
                            } else {
                                break;
                            }
                        }

                        context.setVelocity(true);

                        velocityBlock.append(varBlock);

                        return i;
                    }
                }
            }
        }

        context.setVelocity(false);

        return currentIndex;
    }

    private int getMethod(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
    {
        StringBuffer methodBlock = new StringBuffer(".");

        int i = currentIndex + 1;

        // A JAVA method starts with [a-zA-Z]
        if (Character.isLetter(array[i])) {
            for (; i < array.length; ++i) {
                if (array[i] == '(') {
                    i = getMethodParameters(array, i, methodBlock, context);
                    break;
                } else if (!Character.isLetterOrDigit(array[i])) {
                    break;
                }

                methodBlock.append(array[i]);
            }

            context.setVelocity(true);
        } else {
            context.setVelocity(false);
        }

        if (context.isVelocity()) {
            velocityBlock.append(methodBlock);
        } else {
            i = currentIndex;
        }

        return i;
    }

    private int getTableElement(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        velocityBlock.append('[');

        int i = currentIndex + 1;

        int depth = 1;

        for (; i < array.length;) {
            if (array[i] == ']') {
                --depth;
                if (depth == 0) {
                    velocityBlock.append(array[i]);
                    ++i;
                    break;
                }
            } else if (array[i] == '[') {
                ++depth;
            } else if (array[i] == '"') {
                i = getEscape(array, i, velocityBlock, '"', context);
                continue;
            } else if (array[i] == '\'') {
                i = getEscape(array, i, velocityBlock, '\'', context);
                continue;
            }

            velocityBlock.append(array[i]);
            ++i;
        }

        context.setVelocity(true);

        return i;
    }

    private int getMethodParameters(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        velocityBlock.append('(');

        int i = currentIndex + 1;

        int depth = 1;

        for (; i < array.length;) {
            if (array[i] == ')') {
                --depth;
                if (depth == 0) {
                    velocityBlock.append(array[i]);
                    ++i;
                    break;
                }
            } else if (array[i] == '(') {
                ++depth;
            } else if (array[i] == '"') {
                i = getEscape(array, i, velocityBlock, '"', context);
                continue;
            } else if (array[i] == '\'') {
                i = getEscape(array, i, velocityBlock, '\'', context);
                continue;
            }

            velocityBlock.append(array[i]);
            ++i;
        }

        context.setVelocity(true);

        return i;
    }

    private int getEscape(char[] array, int currentIndex, StringBuffer velocityBlock, char escapeChar,
        VelocityFilterContext context)
    {
        velocityBlock.append(escapeChar);

        int i = currentIndex + 1;

        boolean isVelocity = false;
        boolean escaped = false;

        for (; i < array.length;) {
            if (!escaped) {
                if (array[i] == '\\') {
                    escaped = true;
                } else if (array[i] == '$') {
                    i = getVar(array, i, velocityBlock, context);
                    if (context.isVelocity()) {
                        isVelocity = true;
                        continue;
                    }
                } else if (array[i] == escapeChar) {
                    velocityBlock.append(array[i]);
                    ++i;
                    break;
                }
            } else {
                escaped = false;
            }

            velocityBlock.append(array[i]);
            ++i;
        }

        context.setVelocity(isVelocity);

        return i;
    }

    private int getWhiteSpaces(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length && Character.isWhitespace(array[i]); ++i) {
            velocityBlock.append(array[i]);
        }

        return i;
    }

    public static void appendVelocityOpen(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent("{{velocity}}" + (nl ? "\n" : "")
            + "{{html clean=\"false\" wiki=\"true\"}}" + (nl ? "\n" : ""), VELOCITYOPEN_SUFFIX, true));
    }

    public static void appendVelocityClose(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent((nl ? "\n" : "") + "{{/html}}" + (nl ? "\n" : "")
            + "{{/velocity}}", VELOCITYCLOSE_SUFFIX, true));
    }

    private static class VelocityFilterContext
    {
        private boolean velocity = false;

        private boolean inline = true;

        private boolean conversion = false;

        private int velocityDepth = 0;

        private FilterContext filterContext;

        private boolean protectedBlock;

        public VelocityFilterContext(FilterContext filterContext)
        {
            this.filterContext = filterContext;
        }

        public boolean isVelocity()
        {
            return this.velocity;
        }

        public void setVelocity(boolean velocity)
        {
            this.velocity = velocity;
        }

        public boolean isConversion()
        {
            return this.conversion;
        }

        public void setConversion(boolean conversion)
        {
            this.conversion = conversion;
        }

        public boolean isInline()
        {
            return this.inline;
        }

        public void setInline(boolean inline)
        {
            this.inline = inline;
        }

        public int getVelocityDepth()
        {
            return this.velocityDepth;
        }

        public void pushVelocityDepth()
        {
            ++this.velocityDepth;
        }

        public void popVelocityDepth()
        {
            --this.velocityDepth;
        }

        public FilterContext getFilterContext()
        {
            return this.filterContext;
        }

        public boolean isProtectedBlock()
        {
            return this.protectedBlock;
        }

        public void setProtectedBlock(boolean protectedBlock)
        {
            this.protectedBlock = protectedBlock;
        }
    }
}
