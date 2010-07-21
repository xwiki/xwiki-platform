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
package org.xwiki.rendering.internal.parser.xwiki10.velocity;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.logging.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.internal.parser.xwiki10.HTMLFilter;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.velocity.internal.util.InvalidVelocityException;
import org.xwiki.velocity.internal.util.VelocityBlock;
import org.xwiki.velocity.internal.util.VelocityParser;
import org.xwiki.velocity.internal.util.VelocityParserContext;

/**
 * Extends {@link VelocityParser} for specific needs.
 * 
 * @version $Id$
 */
public class ExtendedVelocityParser extends VelocityParser
{
    private ComponentManager componentManager;

    public ExtendedVelocityParser(ComponentManager componentManager, Logger logger)
    {
        super(logger);

        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.internal.util.VelocityParser#getDirective(char[], int, java.lang.StringBuffer,
     *      org.xwiki.velocity.internal.util.VelocityParserContext)
     */
    @Override
    public int getDirective(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
        throws InvalidVelocityException
    {
        int i = currentIndex + 1;

        ExtendedVelocityParserContext econtext = (ExtendedVelocityParserContext) context;

        // Get macro name
        StringBuffer directiveNameBuffer = new StringBuffer();
        i = getDirectiveName(array, i, directiveNameBuffer, null, context);
        String directiveName = directiveNameBuffer.toString();

        StringBuffer directiveBuffer = new StringBuffer();

        if (VELOCITYDIRECTIVE_ALL.contains(directiveName)) {
            // get the velocity directive
            if (VELOCITYDIRECTIVE_END.contains(directiveName)) {
                if ("macro".equals(context.getCurrentElement().getName())) {
                    HTMLFilter.appendHTMLClose(directiveBuffer, econtext.getFilterContext(), true);
                }

                context.popVelocityElement();
            }

            directiveBuffer.append(array, currentIndex, i - currentIndex);

            if (!VELOCITYDIRECTIVE_NOPARAM.contains(directiveName)) {
                // Skip spaces
                i = getSpaces(array, i, directiveBuffer, context);

                if (i < array.length && array[i] == '(') {
                    // Skip condition
                    i = getMethodParameters(array, i, directiveBuffer, context);
                } else {
                    throw new InvalidVelocityException();
                }
            }

            // consume the end of the line
            i = getDirectiveEndOfLine(array, i, directiveBuffer, context);

            econtext.setVelocity(true);
            context.setType(VelocityBlock.VelocityType.DIRECTIVE);

            if (VELOCITYDIRECTIVE_BEGIN.contains(directiveName)) {
                context.pushVelocityElement(new VelocityBlock(directiveName, VelocityBlock.VelocityType.DIRECTIVE));

                if ("macro".equals(directiveName)) {
                    HTMLFilter.appendHTMLOpen(directiveBuffer, econtext.getFilterContext(), true);
                }
            }
        } else {
            directiveBuffer.append(array, currentIndex, i - currentIndex);

            // Skip spaces
            i = getSpaces(array, i, directiveBuffer, context);

            if (i < array.length && array[i] == '(') {
                ((ExtendedVelocityParserContext) context).setInline(true);

                List<String> parameters = new ArrayList<String>();
                // Get condition
                i = getMacroParameters(array, i, directiveBuffer, parameters, econtext);
                String convertedMacro = convertMacro(directiveName, parameters, econtext);

                if (convertedMacro != null) {
                    // Apply conversion
                    directiveBuffer.setLength(0);
                    directiveBuffer.append(convertedMacro);
                } else {
                    econtext.setVelocity(true);
                    context.setType(VelocityBlock.VelocityType.MACRO);
                }
            } else {
                throw new InvalidVelocityException();
            }
        }

        velocityBlock.append(directiveBuffer);

        return i;
    }

    public String convertMacro(String name, List<String> parameters, ExtendedVelocityParserContext context)
    {
        String convertedMacro = null;

        try {
            VelocityMacroConverter currentMacro = this.componentManager.lookup(VelocityMacroConverter.class, name);

            convertedMacro = currentMacro.convert(name, parameters, context.getFilterContext());

            if (convertedMacro != null) {
                context.setInline(currentMacro.isInline());
                context.setProtectedBlock(currentMacro.protectResult());

                context.setConversion(true);
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

        return convertedMacro;
    }

    public int getMacroParameters(char[] array, int currentIndex, StringBuffer velocityBlock,
        List<String> parameterList, ExtendedVelocityParserContext context)
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.internal.util.VelocityParser#getSimpleComment(char[], int, java.lang.StringBuffer,
     *      org.xwiki.velocity.internal.util.VelocityParserContext)
     */
    @Override
    public int getSimpleComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        ((ExtendedVelocityParserContext) context).setVelocity(true);
        ((ExtendedVelocityParserContext) context).setInline(true);

        return super.getSimpleComment(array, currentIndex, velocityBlock, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.internal.util.VelocityParser#getMultilinesComment(char[], int, java.lang.StringBuffer,
     *      org.xwiki.velocity.internal.util.VelocityParserContext)
     */
    @Override
    public int getMultilinesComment(char[] array, int currentIndex, StringBuffer velocityBlock,
        VelocityParserContext context)
    {
        ((ExtendedVelocityParserContext) context).setVelocity(true);
        ((ExtendedVelocityParserContext) context).setInline(true);

        return super.getMultilinesComment(array, currentIndex, velocityBlock, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.internal.util.VelocityParser#getEscape(char[], int, java.lang.StringBuffer,
     *      org.xwiki.velocity.internal.util.VelocityParserContext)
     */
    @Override
    public int getEscape(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
    {
        char escapeChar = array[currentIndex];

        int i = currentIndex + 1;

        boolean isVelocity = false;
        boolean escaped = false;

        for (; i < array.length;) {
            if (!escaped) {
                if (array[i] == '\\') {
                    escaped = true;
                } else if (array[i] == '$') {
                    try {
                        i = getVar(array, i, null, context);
                        isVelocity = true;
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

        ((ExtendedVelocityParserContext) context).setVelocity(isVelocity);

        return i;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.velocity.internal.util.VelocityParser#getVar(char[], int, java.lang.StringBuffer,
     *      org.xwiki.velocity.internal.util.VelocityParserContext)
     */
    @Override
    public int getVar(char[] array, int currentIndex, StringBuffer velocityBlock, VelocityParserContext context)
        throws InvalidVelocityException
    {
        StringBuffer varName = new StringBuffer();

        int i = super.getVar(array, currentIndex, varName, null, context);

        if (velocityBlock != null) {
            if ("msg".equals(varName.toString())) {
                // Make sure translated messages are protected since it can contain html
                velocityBlock.append("{{html clean=\"false\"}}");
                velocityBlock.append(array, currentIndex, i - currentIndex);
                velocityBlock.append("{{/html}}");
            } else {
                velocityBlock.append(array, currentIndex, i - currentIndex);
            }
        }

        ((ExtendedVelocityParserContext) context).setVelocity(true);

        return i;
    }
}
