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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Wiki UI Extension renderer.
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
    private String roleHint;

    /**
     * Used to transform the macros within the extension content.
     */
    private Transformation macroTransformation;

    /**
     * The xdom from the parsed content, we keep it in order to parse it only once.
     */
    private XDOM xdom;

    /**
     * The syntax the extension content is written in.
     */
    private Syntax syntax;

    /**
     * Default constructor.
     *
     * @param roleHint hint of the UI extension this renderer is bound to
     * @param content content to render
     * @param syntax syntax in which the content is written
     * @param cm the component manager
     * @throws WikiComponentException if some required components can't be found in the Component Manager
     */
    public WikiUIExtensionRenderer(String roleHint, String content, Syntax syntax, ComponentManager cm)
        throws WikiComponentException
    {
        this.roleHint = roleHint;
        this.syntax = syntax;

        try {
            this.macroTransformation = cm.<Transformation>getInstance(Transformation.class, "macro");
            ContentParser contentParser = cm.getInstance(ContentParser.class);
            this.xdom = contentParser.parse(content, syntax);
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(
                "Failed to get an instance for a component role required by Wiki Components.", e);
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
            TransformationContext transformationContext = new TransformationContext(xdom, syntax);
            transformationContext.setId(roleHint);
            macroTransformation.transform(transformedXDOM, transformationContext);
        } catch (TransformationException e) {
            LOGGER.warn("Error while executing wiki component macro transformation for extension [{}]", roleHint);
        }

        return new CompositeBlock(transformedXDOM.getChildren());
    }
}
