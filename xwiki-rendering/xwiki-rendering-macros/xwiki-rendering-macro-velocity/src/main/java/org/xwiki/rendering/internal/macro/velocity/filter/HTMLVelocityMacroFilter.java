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
package org.xwiki.rendering.internal.macro.velocity.filter;

import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.velocity.internal.util.InvalidVelocityException;
import org.xwiki.velocity.internal.util.VelocityParser;
import org.xwiki.velocity.internal.util.VelocityParserContext;
import org.xwiki.velocity.internal.util.VelocityParserContext.VelocityType;

/**
 * Replace each white space/new lines group by a space and inject $nl and $sp bindings in {@link VelocityContext} which
 * are used to respectively force a new line or a space before executing the velocity script. The bindings are removed
 * after script execution.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component("html")
public class HTMLVelocityMacroFilter extends AbstractLogEnabled implements VelocityMacroFilter, Initializable
{
    /**
     * The name of the new line binding.
     */
    private static final String BINDING_NEWLINE = "nl";

    /**
     * The name of the space binding.
     */
    private static final String BINDING_SPACE = "sp";

    /**
     * A white spaces group.
     */
    private static final Pattern WHITESPACES_PATTERN = Pattern.compile("\\s+", Pattern.MULTILINE);

    /**
     * Used to parser content to clean and match system directives and $nl variables.
     */
    private VelocityParser velocityParser;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.velocityParser = new VelocityParser(getLogger());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter#after(java.lang.String,
     *      org.apache.velocity.VelocityContext)
     */
    public String before(String content, VelocityContext velocityContext)
    {
        // Add bindings
        velocityContext.put(BINDING_NEWLINE, "\n");
        velocityContext.put(BINDING_SPACE, " ");

        return clean(content);
    }

    /**
     * @param content the content to clean
     * @return the cleaned content
     */
    public String clean(String content)
    {
        StringBuffer contentBuffer = new StringBuffer();

        char[] array = content.toCharArray();

        VelocityParserContext context = new VelocityParserContext();

        int i = 0;
        int beginIndex = 0;
        while (i < array.length) {
            if (array[i] == '#') {
                try {
                    int endIndex = i;

                    i = this.velocityParser.getKeyWord(array, i, null, context);

                    if (array[i - 1] == '\n') {
                        // append before velocity
                        contentBuffer.append(cleanText(contentBuffer, new String(array, beginIndex, endIndex
                            - beginIndex), context.getType() == VelocityType.DIRECTIVE));

                        // append velocity
                        contentBuffer.append(array, endIndex, i - endIndex);

                        beginIndex = i;
                    }

                    continue;
                } catch (InvalidVelocityException e) {
                    getLogger().debug("Not a valid velocity keyword at char [" + i + "]", e);
                }
            }

            ++i;
        }

        contentBuffer.append(cleanText(contentBuffer, new String(array, beginIndex, i - beginIndex),
            context.getType() == VelocityType.DIRECTIVE));

        return contentBuffer.toString();
    }

    /**
     * Clean between Velocity directives.
     * 
     * @param buffer the previous buffer
     * @param content the content to clean
     * @param indented indicate if the indentation white space has to be remove
     * @return the cleaned text
     */
    private String cleanText(StringBuffer buffer, String content, boolean indented)
    {
        // clean groups of white spaces
        String cleanedContent = cleanWhiteSpaces(content);

        // remove first white spaces when it's before a velocity directive consuming newline (for nice
        // indentation)
        if (cleanedContent.length() > 0 && cleanedContent.charAt(0) == ' ' && indented
            && buffer.charAt(buffer.length() - 1) == '\n') {
            cleanedContent = cleanedContent.substring(1);
        }

        return cleanedContent;
    }

    /**
     * Clean white spaces groups and around $nl variable.
     * 
     * @param content the content to clean
     * @return the cleaned content
     */
    public String cleanWhiteSpaces(String content)
    {
        StringBuffer contentBuffer = new StringBuffer();

        char[] array = content.toCharArray();

        VelocityParserContext context = new VelocityParserContext();

        int i = 0;
        int beginIndex = 0;
        while (i < array.length) {
            if (array[i] == '$') {
                try {
                    int endIndex = i;

                    StringBuffer varName = new StringBuffer();
                    i = this.velocityParser.getVar(array, i, varName, null, context);

                    if (varName.toString().equals(BINDING_NEWLINE)) {
                        beginIndex += getLeadingNewLines(array, endIndex - 1, beginIndex);
                        // append before velocity
                        contentBuffer.append(WHITESPACES_PATTERN.matcher(
                            new String(array, beginIndex, endIndex - beginIndex
                                - getTrailingWhiteSpaces(array, beginIndex, endIndex - 1))).replaceAll(" "));

                        // append velocity
                        contentBuffer.append("${" + BINDING_NEWLINE + "}");

                        beginIndex = i;
                    }

                    continue;
                } catch (InvalidVelocityException e) {
                    getLogger().debug("Not a valid velocity variable at char [" + i + "]", e);
                }
            }

            ++i;
        }

        if (contentBuffer.length() > 0) {
            beginIndex += getLeadingNewLines(array, i - 1, beginIndex);
        }

        contentBuffer
            .append(WHITESPACES_PATTERN.matcher(new String(array, beginIndex, i - beginIndex)).replaceAll(" "));

        return contentBuffer.toString();

    }

    /**
     * @param array the source to parse
     * @param endIndex the end index of the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @return the number of leading new lines
     */
    private int getLeadingNewLines(char[] array, int endIndex, int currentIndex)
    {
        int beginIndex = currentIndex;

        while (beginIndex <= endIndex && Character.isWhitespace(array[beginIndex])) {
            ++beginIndex;
        }

        return beginIndex - currentIndex;
    }

    /**
     * @param array the source to parse
     * @param beginIndex the begin index of the source to parse
     * @param currentIndex the current index in the <code>array</code>
     * @return the number of trailing white spaces
     */
    private int getTrailingWhiteSpaces(char[] array, int beginIndex, int currentIndex)
    {
        int endIndex = currentIndex;

        while (endIndex >= beginIndex && Character.isWhitespace(array[endIndex])) {
            --endIndex;
        }

        return currentIndex - endIndex;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter#before(java.lang.String,
     *      org.apache.velocity.VelocityContext)
     */
    public String after(String content, VelocityContext velocityContect)
    {
        velocityContect.remove(BINDING_NEWLINE);
        velocityContect.remove(BINDING_SPACE);

        return content;
    }
}
