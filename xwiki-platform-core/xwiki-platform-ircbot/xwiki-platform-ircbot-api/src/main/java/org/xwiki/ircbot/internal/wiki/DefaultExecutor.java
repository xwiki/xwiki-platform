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
package org.xwiki.ircbot.internal.wiki;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.Event;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.internal.transformation.macro.MacroErrorManager;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Renders XDOM and send the result to the IRC Channel.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class DefaultExecutor implements WikiIRCModel.Executor
{
    /**
     * @see #DefaultExecutor
     */
    private RenderingContext renderingContext;

    /**
     * @see #DefaultExecutor
     */
    private Transformation macroTransformation;

    /**
     * @see #DefaultExecutor
     */
    private BlockRenderer plainTextBlockRenderer;

    /**
     * Used to find out if the rendered script content has some macro errors or not. If so then we throw an exception
     * to let the user know that the IRC Bot Listener has failed somewhere...
     */
    private MacroErrorManager macroErrorManager = new MacroErrorManager();

    /**
     * @see DefaultExecutor
     */
    private XDOM xdom;

    /**
     * @see DefaultExecutor
     */
    private Syntax syntax;

    /**
     * @see DefaultExecutor
     */
    private Event event;

    /**
     * @param xdom the XDOM to render
     * @param syntax the Syntax in which the Macros are written
     * @param event the IRC Bot Event that the Wiki Bot Listener is responding to and that we use to send back the
     *        rendered content to the IRC Channel
     * @param renderingContext the rendering context we need to keep updated for proper right management.
     * @param macroTransformation the Macro transformation to transform the passed XDOM and execute the Macros in it
     * @param plainTextBlockRenderer the Renderer to use to transform the XDOM into some plain text to send to the IRC
     */
    public DefaultExecutor(XDOM xdom, Syntax syntax, Event event, RenderingContext renderingContext,
        Transformation macroTransformation, BlockRenderer plainTextBlockRenderer)
    {
        this.xdom = xdom;
        this.syntax = syntax;
        this.event = event;
        this.renderingContext = renderingContext;
        this.macroTransformation = macroTransformation;
        this.plainTextBlockRenderer = plainTextBlockRenderer;
    }

    @Override
    public void execute() throws Exception
    {
        String result = renderContent(this.xdom, this.syntax);
        if (!StringUtils.isEmpty(result) && this.event != null) {
            this.event.respond(result);
        }
    }

    /**
     * Renders the content in plain text by executing the macros. This content will then be sent to the IRC Channel.
     *
     * @param xdom the parsed content to render and on which to apply the Macro transformation
     * @param syntax the Syntax in which the macros in the XDOM are written in
     * @return the plain text result
     * @throws TransformationException if the Macro transformation fails somewhere
     * @throws IRCBotException if one of the Macros failed to execute
     */
    private String renderContent(XDOM xdom, Syntax syntax) throws TransformationException, IRCBotException
    {
        // Important: we clone the XDOM so that the transformation will not modify it. Otherwise next time
        // this listener runs, it'll simply return the already transformed XDOM.
        XDOM temporaryXDOM = xdom.clone();

        // Execute the Macro Transformation on XDOM and send the result to the IRC server
        TransformationContext txContext = new TransformationContext(temporaryXDOM, syntax);
        ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation, txContext, temporaryXDOM);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.plainTextBlockRenderer.render(temporaryXDOM, printer);

        // Verify if there are any errors in the transformed macro and if throw an exception so that it can be logged
        // down the line
        if (this.macroErrorManager.containsError(temporaryXDOM)) {
            throw new IRCBotException(String.format("Macro error when rendering Wiki Bot Listener content [%s]",
                printer.toString()));
        }

        return StringUtils.trim(printer.toString());
    }
}
