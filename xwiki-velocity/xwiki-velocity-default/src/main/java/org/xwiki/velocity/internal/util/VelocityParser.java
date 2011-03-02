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
package org.xwiki.velocity.internal.util;

import java.util.HashSet;
import java.util.Set;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.logging.Logger;

/**
 * Provide helpers to parse velocity scripts.
 * 
 * @version $Id$
 */
public class VelocityParser extends AbstractLogEnabled
{
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

    static {
        VELOCITYDIRECTIVE_BEGIN.add("if");
        VELOCITYDIRECTIVE_BEGIN.add("foreach");
        VELOCITYDIRECTIVE_BEGIN.add("literal");
        VELOCITYDIRECTIVE_BEGIN.add("macro");
        VELOCITYDIRECTIVE_BEGIN.add("define");

        VELOCITYDIRECTIVE_END.add("end");

        VELOCITYDIRECTIVE_PARAM.addAll(VELOCITYDIRECTIVE_BEGIN);
        VELOCITYDIRECTIVE_PARAM.add("set");
        VELOCITYDIRECTIVE_PARAM.add("elseif");
        VELOCITYDIRECTIVE_PARAM.add("evaluate");
        VELOCITYDIRECTIVE_PARAM.add("include");

        VELOCITYDIRECTIVE_NOPARAM.addAll(VELOCITYDIRECTIVE_END);
        VELOCITYDIRECTIVE_NOPARAM.add("else");
        VELOCITYDIRECTIVE_NOPARAM.add("break");
        VELOCITYDIRECTIVE_NOPARAM.add("stop");

        VELOCITYDIRECTIVE_ALL.addAll(VELOCITYDIRECTIVE_PARAM);
        VELOCITYDIRECTIVE_ALL.addAll(VELOCITYDIRECTIVE_NOPARAM);
    }

    /**
     * @param logger the logger to use
     */
    public VelocityParser(Logger logger)
    {
        enableLogging(logger);
    }

    /**
     * Get any valid Velocity block starting with a sharp character (#if, #somemaccro(), ##comment etc.).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getKeyWord(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
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

    /**
     * Get any valid Velocity block starting with a sharp character except comments.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getDirective(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
        throws InvalidVelocityException
    {
        int i = currentIndex + 1;

        // Get macro name
        StringBuffer directiveNameBuffer = new StringBuffer();
        i = getDirectiveName(array, i, directiveNameBuffer, null, context);

        String directiveName = directiveNameBuffer.toString();

        if (!VELOCITYDIRECTIVE_NOPARAM.contains(directiveName)) {
            // Skip spaces
            while (i < array.length && array[i] == ' ') {
                ++i;
            }

            if (i < array.length && array[i] == '(') {
                // Skip condition
                i = getMethodParameters(array, i, null, context);
            } else {
                throw new InvalidVelocityException();
            }
        }

        if (VELOCITYDIRECTIVE_ALL.contains(directiveName)) {
            if (VELOCITYDIRECTIVE_BEGIN.contains(directiveName)) {
                context.pushVelocityElement(new VelocityBlock(directiveName, VelocityBlock.VelocityType.DIRECTIVE));
            } else if (VELOCITYDIRECTIVE_END.contains(directiveName)) {
                context.popVelocityElement();
            }

            // consume the end of the line
            i = getDirectiveEndOfLine(array, i, null, context);

            context.setType(VelocityBlock.VelocityType.DIRECTIVE);
        } else {
            context.setType(VelocityBlock.VelocityType.MACRO);
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Get a valid Velocity identifier used for variable of macro.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getVelocityIdentifier(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context) throws InvalidVelocityException
    {
        // The first character of an identifier must be a [a-zA-Z]
        if (!Character.isLetter(array[currentIndex])) {
            throw new InvalidVelocityException();
        }

        int i = currentIndex + 1;

        while (i < array.length && array[i] != '}' && isValidVelocityIdentifierChar(array[i])) {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Indicate if the provided character is valid in a velocity identifier.
     * 
     * @param c the character
     * @return true if the character is valid
     */
    public boolean isValidVelocityIdentifierChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    /**
     * Get a Velocity directive name block. It's different from
     * {@link #getVelocityIdentifier(char[], int, StringBuffer, VelocityParserContext)} because is include the optional
     * <code>{</code> and <code>}</code>.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param directiveName the buffer where to append the name of the directive
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getDirectiveName(char[] array, int currentIndex, StringBuffer directiveName, StringBuffer velocityBlock,
        VelocityParserContext context) throws InvalidVelocityException
    {
        int i = currentIndex;

        if (i == array.length) {
            throw new InvalidVelocityException();
        }

        if (array[i] == '{') {
            ++i;
        }

        i = getVelocityIdentifier(array, i, directiveName, context);

        if (i < array.length && array[i] == '}') {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Get the newline consumed by Velocity directive other than macros.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getDirectiveEndOfLine(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
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

    /**
     * Get comment single line comment (starting with <code>##</code>).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getSimpleComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        int i = currentIndex + 2;

        while (i < array.length && array[i - 1] != '\n') {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        context.setType(VelocityBlock.VelocityType.COMMENT);

        return i;
    }

    /**
     * Get multilines comment (between <code>#*</code> and <code>*#</code>).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getMultilinesComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        int i = currentIndex + 2;

        while (i < array.length && (array[i - 1] != '#' || array[i - 2] != '*')) {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        context.setType(VelocityBlock.VelocityType.COMMENT);

        return i;
    }

    /**
     * Get any valid Velocity starting with a <code>$</code>.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getVar(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
        throws InvalidVelocityException
    {
        return getVar(array, currentIndex, null, velocityBlock, context);
    }

    /**
     * Get any valid Velocity starting with a <code>$</code>.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param varName the buffer where to append the name of the variable
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getVar(char[] array, int currentIndex, StringBuffer varName, StringBuffer velocityBlock,
        VelocityParserContext context) throws InvalidVelocityException
    {
        if (isVarEscaped(array, currentIndex)) {
            throw new InvalidVelocityException();
        }

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
        i = getVelocityIdentifier(array, i, varName, context);

        // get the method(s)
        i = followVar(array, i, fullSyntax, context);

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        context.setType(VelocityBlock.VelocityType.VAR);

        return i;
    }

    /**
     * Look in previous characters of the array to find if the current var is escaped (like \$var).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @return the parser context to put some informations
     */
    private boolean isVarEscaped(char[] array, int currentIndex)
    {
        int i = currentIndex - 1;

        while (i >= 0 && array[i] == '\\') {
            --i;
        }

        return (currentIndex - i) % 2 == 0;
    }

    /**
     * Get the right part of a Velocity variable (the methods and properties starting from the dot).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param fullSyntax indicate if it's between <code>{</code> and <code>}</code>
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    private int followVar(char[] array, int currentIndex, boolean fullSyntax, VelocityParserContext context)
    {
        int i = currentIndex;

        while (i < array.length) {
            if (fullSyntax && array[i] == '}') {
                ++i;
                break;
            } else if (array[i] == '.') {
                try {
                    i = getMethodOrProperty(array, i, null, context);
                } catch (InvalidVelocityException e) {
                    getLogger().debug("Not a valid method at char [" + i + "]", e);
                    break;
                }
            } else if (array[i] == '[') {
                i = getTableElement(array, i, null, context);
                break;
            } else {
                break;
            }
        }

        return i;
    }

    /**
     * Get a velocity method call or a property starting with a <code>.</code>.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     * @throws InvalidVelocityException not a valid velocity block
     */
    public int getMethodOrProperty(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context) throws InvalidVelocityException
    {
        int i = currentIndex + 1;

        // A Velocity method starts with [a-zA-Z]
        if (Character.isLetter(array[i])) {
            for (; i < array.length; ++i) {
                if (array[i] == '(') {
                    i = getMethodParameters(array, i, null, context);
                    break;
                } else if (!Character.isLetterOrDigit(array[i])) {
                    break;
                }
            }
        } else {
            throw new InvalidVelocityException();
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Get a Velocity table.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getTableElement(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        return getParameters(array, currentIndex, velocityBlock, ']', context);
    }

    /**
     * Get the Velocity method parameters (including <code>(</code> and <code>)</code>).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getMethodParameters(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        return getParameters(array, currentIndex, velocityBlock, ')', context);
    }

    /**
     * Get a group of parameters between two characters. Generic version of
     * {@link #getTableElement(char[], int, StringBuffer, VelocityParserContext)} and
     * {@link #getMethodParameters(char[], int, StringBuffer, VelocityParserContext)}.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param endingChar the char to end to
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getParameters(char[] array, int currentIndex, StringBuffer velocityBlock, char endingChar,
        VelocityParserContext context)
    {
        char beginChar = array[currentIndex];

        int i = currentIndex + 1;

        int depth = 1;

        while (i < array.length) {
            if (array[i] == endingChar) {
                --depth;
                if (depth == 0) {
                    ++i;
                    break;
                }
            } else if (array[i] == beginChar) {
                ++depth;
            } else if (array[i] == '"' || array[i] == '\'') {
                i = getEscape(array, i, null, context);
                continue;
            }

            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getEscape(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
    {
        char escapeChar = array[currentIndex];

        int i = currentIndex + 1;

        boolean escaped = false;

        for (; i < array.length;) {
            if (!escaped) {
                if (array[i] == '\\') {
                    escaped = true;
                } else if (array[i] == '$') {
                    try {
                        i = getVar(array, i, null, context);
                        continue;
                    } catch (InvalidVelocityException e) {
                        getLogger().debug("Not a valid variable at char [" + i + "]", e);
                    }
                } else if (array[i] == escapeChar) {
                    ++i;
                    break;
                }
            } else {
                escaped = false;
            }

            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Match a group of {@link Character#isWhitespace(char)}.
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getWhiteSpaces(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
    {
        int i = currentIndex;

        while (i < array.length && Character.isWhitespace(array[i])) {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Match a group of space characters (ASCII 32).
     * 
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getSpaces(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        int i = currentIndex;

        while (i < array.length && Character.isWhitespace(array[i])) {
            ++i;
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getMacroParametersSeparator(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        int i = currentIndex;

        i = getWhiteSpaces(array, i, null, context);
        if (array[i] == ',') {
            i++;
        }
        i = getWhiteSpaces(array, i, null, context);

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * @param array the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @param velocityBlock the buffer where to append matched velocity block
     * @param context the parser context to put some informations
     * @return the index in the <code>array</code> after the matched block
     */
    public int getMacroParameter(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        int i = currentIndex;

        for (; i < array.length; ++i) {
            if (array[i] == '$') {
                try {
                    i = getVar(array, i, null, context);
                    break;
                } catch (InvalidVelocityException e) {
                    getLogger().debug("Not a valid velocity variable at char [" + i + "]", e);
                }
            } else if (array[i] == '"' || array[i] == '\'') {
                i = getEscape(array, i, null, context);
                break;
            } else if (Character.isWhitespace(array[i]) || array[i] == ',') {
                break;
            } else if (array[i] == ')') {
                break;
            }
        }

        if (velocityBlock != null) {
            velocityBlock.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }
}
