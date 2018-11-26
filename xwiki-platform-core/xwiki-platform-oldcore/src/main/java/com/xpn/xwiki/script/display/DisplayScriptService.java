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
package com.xpn.xwiki.script.display;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;

/**
 * Exposes {@link org.xwiki.display.internal.Displayer}s to scripts.
 *
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named(DisplayScriptService.ROLEHINT)
@Singleton
public class DisplayScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "display";

    /**
     * The key used to store the displayer parameters in the display parameter map.
     */
    private static final String DISPLAYER_PARAMETERS_KEY = "displayerParameters";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * The component manager.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The rendering cache.
     */
    @Inject
    private RenderingCache renderingCache;

    /**
     * Execution context handler, needed for accessing the XWikiContext.
     */
    @Inject
    private Execution execution;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    private Syntax getOutputSyntax(Map<String, Object> parameters)
    {
        Syntax outputSyntax = (Syntax) parameters.get("outputSyntax");
        if (outputSyntax == null) {
            String outputSyntaxId = (String) parameters.get("outputSyntaxId");
            if (outputSyntaxId != null) {
                try {
                    outputSyntax = Syntax.valueOf(outputSyntaxId);
                } catch (Exception e) {
                    this.logger.error("Failed to parse output syntax ID [{}].", outputSyntaxId, e);
                    return null;
                }
            } else {
                outputSyntax = renderingContext.getTargetSyntax();
            }
        }

        return outputSyntax;
    }

    /**
     * Displays a document.
     *
     * @param document the document to display
     * @param parameters the display parameters
     * @return the result of displaying the given document
     */
    private String document(Document document, Map<String, Object> parameters, Syntax outputSyntax)
    {
        DocumentDisplayerParameters displayerParameters =
            (DocumentDisplayerParameters) parameters.get(DISPLAYER_PARAMETERS_KEY);
        if (displayerParameters == null) {
            displayerParameters = new DocumentDisplayerParameters();
            displayerParameters.setTargetSyntax(outputSyntax);
        }

        String displayerHint = (String) parameters.get("displayerHint");
        if (displayerHint == null) {
            displayerHint = "configured";
        }
        try {
            DocumentDisplayer displayer = this.componentManager.getInstance(DocumentDisplayer.class, displayerHint);
            return renderXDOM(displayer.display(getDocument(document), displayerParameters), outputSyntax);
        } catch (Exception e) {
            this.logger.error("Failed to display document [{}].", document.getPrefixedFullName(), e);
            return null;
        }
    }

    /**
     * @return a new instance of {@link DocumentDisplayerParameters}
     */
    public DocumentDisplayerParameters createDocumentDisplayerParameters()
    {
        return new DocumentDisplayerParameters();
    }

    /**
     * @param document the document whose content is displayed
     * @return the result of rendering the content of the given document as XHTML using the configured displayer
     * @see #content(Document, Map)
     */
    public String content(Document document)
    {
        return content(document, Collections.<String, Object>emptyMap());
    }

    /**
     * Displays the content of the given document.
     *
     * @param document the document whose content is displayed
     * @param parameters the display parameters
     * @return the result of rendering the content of the given document using the provided parameters
     */
    public String content(Document document, Map<String, Object> parameters)
    {
        XWikiContext context = getXWikiContext();
        String content = null;
        try {
            content = document.getTranslatedContent();
        } catch (XWikiException e) {
            this.logger.warn("Failed to get the translated content of document [{}].", document.getPrefixedFullName(),
                e);
            return null;
        }
        String renderedContent =
            this.renderingCache.getRenderedContent(document.getDocumentReference(), content, context);
        if (renderedContent == null) {
            Map<String, Object> actualParameters = new HashMap<String, Object>(parameters);
            DocumentDisplayerParameters displayerParameters =
                (DocumentDisplayerParameters) parameters.get(DISPLAYER_PARAMETERS_KEY);
            if (displayerParameters == null) {
                displayerParameters = new DocumentDisplayerParameters();
                // Default content display parameters.
                displayerParameters.setExecutionContextIsolated(true);
                displayerParameters.setContentTranslated(true);
            } else if (displayerParameters.isTitleDisplayed()) {
                // Clone because we have to enforce content display.
                displayerParameters = displayerParameters.clone();
            }
            // Ensure the content is displayed.
            displayerParameters.setTitleDisplayed(false);
            Syntax outputSyntax = getOutputSyntax(parameters);
            displayerParameters.setTargetSyntax(outputSyntax);
            actualParameters.put(DISPLAYER_PARAMETERS_KEY, displayerParameters);
            renderedContent = document(document, actualParameters, outputSyntax);
            if (renderedContent != null) {
                this.renderingCache.setRenderedContent(document.getDocumentReference(), content, renderedContent,
                    context);
            }
        }
        return renderedContent;
    }

    /**
     * Displays the document title. If a title has not been provided through the title field, it looks for a section
     * title in the document's content and if not found return the page name. The returned title is also interpreted
     * which means it's allowed to use Velocity, Groovy, etc. syntax within a title.
     *
     * @param document the document whose title is displayed
     * @param parameters the display parameters
     * @return the result of displaying the title of the given document
     */
    public String title(Document document, Map<String, Object> parameters)
    {
        Map<String, Object> actualParameters = new HashMap<String, Object>(parameters);
        DocumentDisplayerParameters displayerParameters =
            (DocumentDisplayerParameters) parameters.get(DISPLAYER_PARAMETERS_KEY);
        if (displayerParameters == null) {
            displayerParameters = new DocumentDisplayerParameters();
            // Default title display parameters.
            displayerParameters.setExecutionContextIsolated(true);
        } else if (!displayerParameters.isTitleDisplayed()) {
            // Clone because we have to enforce title display.
            displayerParameters = displayerParameters.clone();
        }
        // Ensure the title is displayed.
        displayerParameters.setTitleDisplayed(true);
        Syntax outputSyntax = getOutputSyntax(parameters);
        displayerParameters.setTargetSyntax(outputSyntax);
        actualParameters.put(DISPLAYER_PARAMETERS_KEY, displayerParameters);
        return document(document, actualParameters, outputSyntax);
    }

    /**
     * @param document the document whose title is displayed
     * @return the result of rendering the title of the given document as XHTML using the configured displayer
     * @see #title(Document, Map)
     */
    public String title(Document document)
    {
        return title(document, Collections.<String, Object>emptyMap());
    }

    /**
     * Gets a sub-service.
     *
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     * @since 10.11RC1
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ROLEHINT + '.' + serviceName);
    }

    /**
     * Note: This method accesses the low level XWiki document through reflection in order to bypass programming rights.
     *
     * @param document an instance of {@link Document} received from a script
     * @return an instance of {@link DocumentModelBridge} that wraps the low level document object exposed by the given
     *         document API
     */
    private DocumentModelBridge getDocument(Document document)
    {
        try {
            // HACK: We try to access the XWikiDocument instance wrapped by the document API using reflection because we
            // want to bypass the programming rights requirements.
            Field docField = Document.class.getDeclaredField("doc");
            docField.setAccessible(true);
            return (DocumentModelBridge) docField.get(document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access the XWikiDocument instance wrapped by the document API.", e);
        }
    }

    /**
     * Renders the provided XDOM.
     *
     * @param content the XDOM content to render
     * @param targetSyntax the syntax of the rendering result
     * @return the result of rendering the given XDOM
     * @throws XWikiException if an exception occurred during the rendering process
     */
    private String renderXDOM(XDOM content, Syntax targetSyntax) throws XWikiException
    {
        try {
            BlockRenderer renderer = this.componentManager.getInstance(BlockRenderer.class, targetSyntax.toIdString());
            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(content, printer);
            return printer.toString();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to render XDOM to syntax [" + targetSyntax + "]", e);
        }
    }

    /**
     * @return the XWiki context
     * @deprecated avoid using this method; try using the document access bridge instead
     */
    @Deprecated
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
