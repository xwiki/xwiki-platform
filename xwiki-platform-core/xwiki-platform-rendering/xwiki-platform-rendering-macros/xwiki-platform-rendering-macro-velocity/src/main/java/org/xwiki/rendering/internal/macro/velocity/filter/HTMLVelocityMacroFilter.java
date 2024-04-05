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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.internal.util.InvalidVelocityException;
import org.xwiki.velocity.internal.util.VelocityBlock.VelocityType;
import org.xwiki.velocity.internal.util.VelocityParser;
import org.xwiki.velocity.internal.util.VelocityParserContext;

/**
 * Replace each white space/new lines group by a space and inject $nl and $sp bindings in {@link VelocityContext} which
 * are used to respectively force a new line or a space before executing the velocity script. The bindings are removed
 * after script execution.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("html")
@Singleton
public class HTMLVelocityMacroFilter implements VelocityMacroFilter, Initializable
{
    /**
     * The name of the new line binding.
     */
    private static final String BINDING_NEWLINE = "nl";

    /**
     * The value of the $nl binding.
     */
    private static final String NEWLINE = "\n";

    /**
     * The name of the space binding.
     */
    private static final String BINDING_SPACE = "sp";

    /**
     * The value of the $sp binding.
     */
    private static final String SPACE = " ";

    /**
     * Match not UNIX new lines to replace them.
     */
    private static final Pattern MSNEWLINE_PATTERN = Pattern.compile("\\r\\n|\\r");

    /**
     * The logger to use for logging.
     */
    @Inject
    private Logger logger;

    /**
     * Used to parser content to clean and match system directives and $nl variables.
     */
    private VelocityParser velocityParser;

    @Override
    public void initialize() throws InitializationException
    {
        this.velocityParser = new VelocityParser();
    }

    @Override
    public boolean isPreparationSupported()
    {
        return true;
    }

    private void before(VelocityContext velocityContext)
    {
        // Add bindings
        velocityContext.put(BINDING_NEWLINE, NEWLINE);
        velocityContext.put(BINDING_SPACE, SPACE);
    }

    @Override
    public String before(String content, VelocityContext velocityContext)
    {
        before(velocityContext);

        return prepare(content);
    }

    @Override
    public void before(VelocityTemplate content, VelocityContext velocityContext)
    {
        before(velocityContext);
    }

    @Override
    public String prepare(String content)
    {
        return clean(content);
    }

    /**
     * Clean whites spaces in the velocity macro content.
     * <p>
     * Here a the rules:
     * <ul>
     * <li>any group of white spaces is replaced by a space</li>
     * <li>all white spaces after a velocity directive consuming following newline (#if, #set, etc.) are removed (no
     * replacement)</li>
     * <li>all white spaces before or after $nl are removed (no replacement)</li>
     * <li>all white spaces at the beginning and at the end of the content are removed (no replacement)</li>
     * <li>all velocity comments are removed</li>
     * </ul>
     * 
     * @param content the content to clean
     * @return the cleaned content
     */
    public String clean(String content)
    {
        StringBuffer contentBuffer = new StringBuffer();

        char[] array = MSNEWLINE_PATTERN.matcher(content).replaceAll(NEWLINE).toCharArray();

        VelocityParserContext context = new VelocityParserContext();
        FilterContext filterContext = new FilterContext();

        int i = 0;
        while (i < array.length) {
            try {
                if (array[i] == '#') {
                    i = cleanKeyWord(contentBuffer, array, i, context, filterContext);

                    continue;
                } else if (array[i] == '$') {
                    i = cleanVar(contentBuffer, array, i, context, filterContext);

                    continue;
                } else if (Character.isWhitespace(array[i])) {
                    if (!filterContext.removeWhiteSpaces && contentBuffer.length() > 0) {
                        filterContext.foundWhiteSpace = true;
                    }

                    ++i;

                    continue;
                }
            } catch (InvalidVelocityException e) {
                this.logger.debug("Not a valid velocity keyword at char [" + i + "]", e);
            }

            flushWhiteSpaces(contentBuffer, filterContext, false);

            contentBuffer.append(array[i]);

            ++i;
        }

        flushWhiteSpaces(contentBuffer, filterContext, true);

        return contentBuffer.toString();
    }

    /**
     * Handle velocity comments and directive.
     * 
     * @param contentBuffer the final result buffer
     * @param array the source
     * @param currentIndex the current index in the source
     * @param context the velocity parser context
     * @param filterContext the filter context
     * @return the index after the comment or directive
     * @throws InvalidVelocityException not velocity
     */
    private int cleanKeyWord(StringBuffer contentBuffer, char[] array, int currentIndex, VelocityParserContext context,
        FilterContext filterContext) throws InvalidVelocityException
    {
        int i = this.velocityParser.getKeyWord(array, currentIndex, null, context);

        if (context.getType() != VelocityType.COMMENT) {
            if (context.getType() == VelocityType.DIRECTIVE) {
                if (filterContext.wsGroup.length() == 0) {
                    flushWhiteSpaces(filterContext.wsGroup, filterContext, false);
                }

                filterContext.wsGroup.append(array, currentIndex, i - currentIndex);

                filterContext.removeWhiteSpaces = true;
            } else {
                flushWhiteSpaces(contentBuffer, filterContext, false);

                contentBuffer.append(array, currentIndex, i - currentIndex);
            }
        }

        return i;
    }

    /**
     * Handle velocity variables.
     * 
     * @param contentBuffer the final result buffer
     * @param array the source
     * @param currentIndex the current index in the source
     * @param context the velocity parser context
     * @param filterContext the filter context
     * @return the index after the variable
     * @throws InvalidVelocityException not velocity
     */
    private int cleanVar(StringBuffer contentBuffer, char[] array, int currentIndex, VelocityParserContext context,
        FilterContext filterContext) throws InvalidVelocityException
    {
        StringBuffer varName = new StringBuffer();
        int i = this.velocityParser.getVar(array, currentIndex, varName, null, context);

        if (varName.toString().equals(BINDING_NEWLINE)) {
            flushWhiteSpaces(contentBuffer, filterContext, true);

            contentBuffer.append("${nl}");
            filterContext.removeWhiteSpaces = true;
        } else {
            flushWhiteSpaces(contentBuffer, filterContext, false);

            contentBuffer.append(array, currentIndex, i - currentIndex);
        }

        return i;
    }

    /**
     * Flush stored velocity directive which does not produce output (like #if, set, etc.). I also append a space if
     * there was a space in the group source. This is not make sure the whole no output velocity content is taken into
     * account when cleaning white spaces to be sure to have only one space at the end between two really generated
     * text.
     * 
     * @param contentBuffer the final result buffer
     * @param filterContext the filter context
     * @param forceNoSpace if true no space is printed (only {@link FilterContext#wsGroup})
     */
    private void flushWhiteSpaces(StringBuffer contentBuffer, FilterContext filterContext, boolean forceNoSpace)
    {
        if (filterContext.wsGroup.length() > 0) {
            boolean space = filterContext.wsGroup.charAt(0) == ' ';

            if (forceNoSpace && space) {
                contentBuffer.append(filterContext.wsGroup, 1, filterContext.wsGroup.length());
            } else {
                contentBuffer.append(filterContext.wsGroup);
            }
            filterContext.wsGroup.setLength(0);
        }

        if (filterContext.foundWhiteSpace && !forceNoSpace) {
            contentBuffer.append(' ');
        }

        filterContext.foundWhiteSpace = false;
        filterContext.removeWhiteSpaces = false;
    }

    @Override
    public String after(String content, VelocityContext velocityContext)
    {
        velocityContext.remove(BINDING_NEWLINE);
        velocityContext.remove(BINDING_SPACE);

        return content;
    }

    /**
     * Used to return store and retrieve some information during the filtering process.
     * 
     * @version $Id$
     */
    static class FilterContext
    {
        /**
         * Indicate if at least one white space has been found.
         */
        private boolean foundWhiteSpace;

        /**
         * Indicate if whites space has to be removed instead of replaced by a unique space.
         */
        private boolean removeWhiteSpaces;

        /**
         * @see #getWsGroup()
         */
        private StringBuffer wsGroup = new StringBuffer();

        /**
         * @return Indicate if at least one white space has been found.
         */
        public boolean isFoundWhiteSpace()
        {
            return this.foundWhiteSpace;
        }

        /**
         * @param foundWhiteSpace Indicate if at least one white space has been found.
         */
        public void setFoundWhiteSpace(boolean foundWhiteSpace)
        {
            this.foundWhiteSpace = foundWhiteSpace;
        }

        /**
         * @return Indicate if whites space has to be removed instead of replaced by a unique space.
         */
        public boolean isRemoveWhiteSpaces()
        {
            return this.removeWhiteSpaces;
        }

        /**
         * @param removeWhiteSpaces Indicate if whites space has to be removed instead of replaced by a unique space.
         */
        public void setRemoveWhiteSpaces(boolean removeWhiteSpaces)
        {
            this.removeWhiteSpaces = removeWhiteSpaces;
        }

        /**
         * @return Used to store velocity directive whish does not generate output until something else is found. It's
         *         used to match a whole group of white space including no output velocity code.
         */
        public StringBuffer getWsGroup()
        {
            return this.wsGroup;
        }
    }
}
