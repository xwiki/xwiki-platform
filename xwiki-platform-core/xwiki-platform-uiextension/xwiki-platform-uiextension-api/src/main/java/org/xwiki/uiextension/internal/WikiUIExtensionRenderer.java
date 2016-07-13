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
package org.xwiki.uiextension.internal;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Parse the UI Extension content as an XDOM, cache it and when asked, apply Rendering Transformations on the XDOM
 * and return the modified XDOM.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class WikiUIExtensionRenderer
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiUIExtensionRenderer.class);

    /**
     * Role hint of the UI extension this renderer is bound to.
     */
    private final String roleHint;

    /**
     * Used to update the rendering context.
     */
    private final RenderingContext renderingContext;

    /**
     * Used to transform the macros within the extension content.
     */
    private final Transformation macroTransformation;

    /**
     * Used to retrieve the XWiki context.
     */
    private final Execution execution;

    /**
     * The xdom from the parsed content, we keep it in order to parse it only once.
     */
    private final XDOM xdom;

    /**
     * Reference to the document holding the extension.
     */
    private final DocumentReference documentReference;

    /**
     * Default constructor.
     *
     * @param roleHint hint of the UI extension this renderer is bound to
     * @param content content to render
     * @param documentReference a reference to the document holding the extension
     * @param cm the component manager
     * @throws WikiComponentException if some required components can't be found in the Component Manager
     */
    public WikiUIExtensionRenderer(String roleHint, String content, DocumentReference documentReference,
        ComponentManager cm) throws WikiComponentException
    {
        this.roleHint = roleHint;

        try {
            this.execution = cm.getInstance(Execution.class);
            this.renderingContext = cm.getInstance(RenderingContext.class);
            this.macroTransformation = cm.<Transformation>getInstance(Transformation.class, "macro");
            ContentParser contentParser = cm.getInstance(ContentParser.class);
            XWikiDocument xdoc = getXWikiContext().getWiki().getDocument(documentReference, getXWikiContext());
            this.xdom = contentParser.parse(content, xdoc.getSyntax(), documentReference);
            this.documentReference = documentReference;
        } catch (ComponentLookupException ex) {
            throw new WikiComponentException(
                "Failed to get an instance for a component role required by Wiki Components.", ex);
        } catch (XWikiException e) {
            throw new WikiComponentException(String.format(
                "Failed to retrieve document [%s]", documentReference), e);
        }
    }

    /**
     * @return the rendered content of the extension
     */
    public CompositeBlock execute()
    {
        // We need to clone the xdom to avoid transforming the original and make it useless after the first
        // transformation
        XDOM transformedXDOM = xdom.clone();

        // Perform macro transformations.
        try {
            // Get the document holding the UIX and put it in the UIX context
            XWikiDocument xdoc = getXWikiContext().getWiki().getDocument(documentReference, getXWikiContext());
            Map<String, Object> uixContext = new HashMap<String, Object>();
            uixContext.put(WikiUIExtension.CONTEXT_UIX_DOC_KEY, xdoc.newDocument(getXWikiContext()));

            // Put the UIX context in the XWiki context
            getXWikiContext().put(WikiUIExtension.CONTEXT_UIX_KEY, uixContext);

            // Transform the macros
            TransformationContext transformationContext = new TransformationContext(xdom, xdoc.getSyntax());
            transformationContext.setId(roleHint);
            ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation,
                transformationContext, transformedXDOM);
        } catch (TransformationException e) {
            LOGGER.warn("Error while executing wiki component macro transformation for extension [{}]", roleHint);
        } catch (XWikiException ex) {
            LOGGER.warn("Failed to retrieve document [{}]", documentReference);
        }

        return new CompositeBlock(transformedXDOM.getChildren());
    }

    /**
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
