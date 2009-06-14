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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.internal.parser.xwiki10.velocity.InvalidVelocityException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Register all Velocity comments in order to protect them from following filters. Protect velocity comments, convert
 * velocity macro into 2.0 macros/syntax and add needed 2.0 velocity macros and convert.
 * <p>
 * See http://velocity.apache.org/engine/releases/velocity-1.6.2/vtl-reference-guide.html
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("velocity")
public class VelocityFilter extends AbstractFilter implements Initializable
{
    public static final String VELOCITY_SUFFIX = "velocity";

    public static final String VELOCITYOPEN_SUFFIX = VELOCITY_SUFFIX + "open";

    public static final String VELOCITYCLOSE_SUFFIX = VELOCITY_SUFFIX + "close";

    public static final String VELOCITYCOMMENT_SUFFIX = VELOCITY_SUFFIX + VelocityFilterContext.VelocityType.COMMENT;

    /**
     * The directives which start a new level which will have to be close by a #end.
     */
    public static final Set<String> VELOCITYDIRECTIVE_BEGIN = new HashSet<String>();

    /**
     * Close an opened level.
     */
    public static final Set<String> VELOCITYDIRECTIVE_END = new HashSet<String>();

    /**
     * Reserved directive containing parameter(s) like #if.
     */
    public static final Set<String> VELOCITYDIRECTIVE_PARAM = new HashSet<String>();

    /**
     * Reserved directives without parameters like #else.
     */
    public static final Set<String> VELOCITYDIRECTIVE_NOPARAM = new HashSet<String>();

    /**
     * All the velocity reserved directives.
     */
    public static final Set<String> VELOCITYDIRECTIVE_ALL = new HashSet<String>();

    public static final String VELOCITY_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VELOCITY_SUFFIX + "\\p{L}*\\d+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYOPEN_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VELOCITYOPEN_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYCLOSE_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VELOCITYCLOSE_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYCONTENT_SPATTERN =
        "(?:" + VELOCITYOPEN_SPATTERN + ".*" + VELOCITYCLOSE_SPATTERN + ")";

    public static final String VELOCITYCOMMENT_SPATTERN =
        "(?:" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENNI + VELOCITYCOMMENT_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String EMPTY_SPATTERN =
        VelocityFilter.VELOCITYOPEN_SPATTERN + "?" + VelocityFilter.VELOCITYCOMMENT_SPATTERN + "*"
            + VelocityFilter.VELOCITYCLOSE_SPATTERN + "?";

    public static final String EMPTYSPACE_SPATTERN =
        "[ \\t]*" + VelocityFilter.VELOCITYOPEN_SPATTERN + "?(?:[ \\t]*" + VelocityFilter.VELOCITYCOMMENT_SPATTERN
            + "*)*" + VelocityFilter.VELOCITYCLOSE_SPATTERN + "?[ \\t]*";

    public static final Pattern VELOCITYOPEN_PATTERN = Pattern.compile(VELOCITYOPEN_SPATTERN);

    public static final Pattern VELOCITYCLOSE_PATTERN = Pattern.compile(VELOCITYCLOSE_SPATTERN);

    public static final Pattern VELOCITYCONTENT_PATTERN = Pattern.compile(VELOCITYCONTENT_SPATTERN, Pattern.DOTALL);

    static {
        VELOCITYDIRECTIVE_BEGIN.add("if");
        VELOCITYDIRECTIVE_BEGIN.add("foreach");
        VELOCITYDIRECTIVE_BEGIN.add("literal");

        VELOCITYDIRECTIVE_END.add("end");

        VELOCITYDIRECTIVE_PARAM.add("if");
        VELOCITYDIRECTIVE_PARAM.add("foreach");
        VELOCITYDIRECTIVE_PARAM.add("set");
        VELOCITYDIRECTIVE_PARAM.add("elseif");
        VELOCITYDIRECTIVE_PARAM.add("define");
        VELOCITYDIRECTIVE_PARAM.add("evaluate");

        VELOCITYDIRECTIVE_NOPARAM.add("end");
        VELOCITYDIRECTIVE_NOPARAM.add("else");
        VELOCITYDIRECTIVE_NOPARAM.add("break");
        VELOCITYDIRECTIVE_NOPARAM.add("stop");

        VELOCITYDIRECTIVE_ALL.addAll(VELOCITYDIRECTIVE_PARAM);
        VELOCITYDIRECTIVE_ALL.addAll(VELOCITYDIRECTIVE_NOPARAM);
    }

    /**
     * Used to lookup macros converters.
     */
    @Requirement
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
            context.setType(null);

            StringBuffer velocityBlock = new StringBuffer();

            try {
                if (c == '#') {
                    i = getKeyWord(array, i, velocityBlock, context);
                } else if (c == '$') {
                    i = getVar(array, i, velocityBlock, context);
                }
            } catch (InvalidVelocityException e) {
                getLogger().debug("Not a valid Velocity block at char [" + i + "]", e);
                context.setVelocity(false);
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
                    .toString(), VELOCITY_SUFFIX + context.getType(), context.isInline()) : velocityBlock);
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
            String unProtectedVelocityContent = filterContext.unProtect(velocityContent);
            String afterVelocityContent = afterVelocityBuffer.toString();

            boolean multilines = unProtectedVelocityContent.indexOf("\n") != -1;

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
        throws InvalidVelocityException
    {
        int i = currentIndex;

        if (i + 1 >= array.length) {
            throw new InvalidVelocityException();
        }

        if (array[i + 1] == '#') {
            // A simple line comment
            i = getSimpleComment(array, currentIndex, velocityBlock, context);
        } else if (array[i + 1] == '*') {
            // A multi lines comment
            i = getMultilinesComment(array, currentIndex, velocityBlock, context);
        } else if (array[i + 1] == '{' || Character.isLetter(array[i + 1])) {
            // A directive
            i = getDirective(array, currentIndex, velocityBlock, context);
        }

        return i;
    }

    private int getDirective(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
        throws InvalidVelocityException
    {
        int i = currentIndex + 1;

        // Get macro name
        StringBuffer directiveNameBuffer = new StringBuffer();
        i = getDirectiveName(array, i, directiveNameBuffer, null);
        String directiveName = directiveNameBuffer.toString();

        StringBuffer directiveBuffer = new StringBuffer();
        directiveBuffer.append(array, currentIndex, i - currentIndex);

        if (VELOCITYDIRECTIVE_ALL.contains(directiveName)) {
            context.setVelocity(true);
            context.setType(VelocityFilterContext.VelocityType.DIRECTIVE);

            // get the velocity directive
            if (VELOCITYDIRECTIVE_BEGIN.contains(directiveName)) {
                context.pushVelocityDepth();
            } else if (VELOCITYDIRECTIVE_END.contains(directiveName)) {
                context.popVelocityDepth();
            }

            if (!VELOCITYDIRECTIVE_NOPARAM.contains(directiveName)) {
                // Skip spaces
                for (; i < array.length && array[i] == ' '; ++i) {
                    directiveBuffer.append(array[i]);
                }

                if (i < array.length) {
                    if (array[i] == '(') {
                        // Skip condition
                        i = getMethodParameters(array, i, directiveBuffer, context);
                    }
                }
            }

            // consume the end of the line
            i = getDirectiveEndOfLine(array, i, directiveBuffer);
        } else {
            // Skip spaces
            for (; i < array.length && array[i] == ' '; ++i) {
                directiveBuffer.append(array[i]);
            }

            if (i < array.length) {
                if (array[i] == '(') {
                    context.setInline(true);

                    List<String> parameters = new ArrayList<String>();
                    // Get condition
                    i = getMacroParameters(array, i, directiveBuffer, parameters, context);
                    String convertedMacro = convertMacro(directiveName, parameters, context);

                    if (convertedMacro != null) {
                        // Apply conversion
                        directiveBuffer.setLength(0);
                        directiveBuffer.append(convertedMacro);
                    } else {
                        context.setVelocity(true);
                        context.setType(VelocityFilterContext.VelocityType.DIRECTIVE);
                    }
                }
            }
        }

        velocityBlock.append(directiveBuffer);

        return i;
    }

    private int getVelocityIdentifier(char[] array, int currentIndex, StringBuffer velocityBlock)
        throws InvalidVelocityException
    {
        if (!Character.isLetter(array[currentIndex])) {
            throw new InvalidVelocityException();
        }

        int i = currentIndex + 1;

        for (; i < array.length && array[i] != '}'
            && (Character.isLetterOrDigit(array[i]) || array[i] == '_' || array[i] == '-'); ++i) {
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getDirectiveName(char[] array, int currentIndex, StringBuffer directiveName, StringBuffer velocityBlock)
        throws InvalidVelocityException
    {
        int i = currentIndex;

        if (i == array.length) {
            throw new InvalidVelocityException();
        }

        if (array[i] == '{') {
            ++i;
        }

        i = getVelocityIdentifier(array, i, directiveName);

        if (i < array.length && array[i] == '}') {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getDirectiveEndOfLine(char[] array, int currentIndex, StringBuffer velocityBlock)
    {
        int i = currentIndex;

        for (; i < array.length; ++i) {
            if (array[i] == '\n') {
                ++i;
                break;
            } else if (!Character.isWhitespace(array[i])) {
                return currentIndex;
            }
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private String convertMacro(String name, List<String> parameters, VelocityFilterContext context)
    {
        String convertedMacro = null;

        try {
            VelocityMacroConverter currentMacro = this.componentManager.lookup(VelocityMacroConverter.class, name);

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
            i = getMacroParametersSeparator(array, i, velocityBlock);

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

    private int getMacroParametersSeparator(char[] array, int currentIndex, StringBuffer velocityBlock)
    {
        int i = currentIndex;

        i = getWhiteSpaces(array, i, null);
        if (array[i] == ',') {
            i++;
        }
        i = getWhiteSpaces(array, i, null);

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    private int getMacroParameter(char[] array, int currentIndex, StringBuffer parameterBlock,
        VelocityFilterContext context)
    {
        int i = currentIndex;

        for (; i < array.length; ++i) {
            if (array[i] == '$') {
                try {
                    i = getVar(array, i, parameterBlock, context);
                    break;
                } catch (InvalidVelocityException e) {
                    getLogger().debug("Not a valid velocity variable at char [" + i + "]", e);
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
        context.setInline(false);
        context.setType(VelocityFilterContext.VelocityType.COMMENT);

        int i = currentIndex + 2;

        for (; i < array.length && array[i - 1] != '\n'; ++i) {
        }

        velocityBlock.append(array, currentIndex, i - currentIndex);

        return i;
    }

    private int getMultilinesComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityFilterContext context)
    {
        context.setVelocity(true);
        context.setInline(false);
        context.setType(VelocityFilterContext.VelocityType.COMMENT);

        int i = currentIndex + 2;

        for (; i < array.length && (array[i - 1] != '#' || array[i - 2] != '*'); ++i) {
        }

        velocityBlock.append(array, currentIndex, i - currentIndex);

        return i;
    }

    private int getVar(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityFilterContext context)
        throws InvalidVelocityException
    {
        int i = currentIndex + 1;

        if (i == array.length) {
            throw new InvalidVelocityException();
        }

        if (array[i] == '!') {
            ++i;
        }

        if (i == array.length) {
            throw new InvalidVelocityException();
        }

        boolean fullSyntax = false;
        if (array[i] == '{') {
            ++i;
            fullSyntax = true;
        }

        if (i == array.length) {
            throw new InvalidVelocityException();
        }

        // get the variable name
        i = getVelocityIdentifier(array, i, null);

        StringBuffer varBlock = new StringBuffer();
        varBlock.append(array, currentIndex, i - currentIndex);

        // get the method
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
        context.setType(VelocityFilterContext.VelocityType.VAR);

        velocityBlock.append(varBlock);

        return i;
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
                    try {
                        i = getVar(array, i, velocityBlock, context);
                        isVelocity = true;
                        continue;
                    } catch (InvalidVelocityException e) {
                        getLogger().debug("Not a valid variable at char [" + i + "]", e);
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

    private int getWhiteSpaces(char[] array, int currentIndex, StringBuffer velocityBlock)
    {
        int i = currentIndex;

        for (; i < array.length && Character.isWhitespace(array[i]); ++i) {
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    public static void appendVelocityOpen(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent("{{velocity filter=\"none\"}}" + (nl ? "\n" : ""),
            VELOCITYOPEN_SUFFIX, true));
    }

    public static void appendVelocityClose(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result
            .append(filterContext.addProtectedContent((nl ? "\n" : "") + "{{/velocity}}", VELOCITYCLOSE_SUFFIX, true));
    }

    private static class VelocityFilterContext
    {
        public enum VelocityType
        {
            COMMENT,
            DIRECTIVE,
            VAR
        }

        private boolean velocity = false;

        private boolean inline = true;

        private boolean conversion = false;

        private int velocityDepth = 0;

        private FilterContext filterContext;

        private boolean protectedBlock;

        private VelocityType type;

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

        public void setType(VelocityType type)
        {
            this.type = type;
        }

        public VelocityType getType()
        {
            return this.type;
        }
    }
}
