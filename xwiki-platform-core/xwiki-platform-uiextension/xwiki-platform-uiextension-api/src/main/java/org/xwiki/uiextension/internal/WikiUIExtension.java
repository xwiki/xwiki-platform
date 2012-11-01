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

import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.velocity.VelocityManager;

/**
 * Represents a dynamic component instance of a UI Extension (ie a UI Extension defined in a Wiki page) that we
 * register against the Component Manager.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class WikiUIExtension implements UIExtension, WikiComponent
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiUIExtension.class);

    /**
     * @see #WikiUIExtension
     */
    private final DocumentReference documentReference;

    /**
     * @see #WikiUIExtension
     */
    private final String name;

    /**
     * @see #WikiUIExtension
     */
    private final String extensionPointId;

    /**
     * @see #WikiUIExtension
     */
    private String roleHint;

    /**
     * @see #WikiUIExtension
     */
    private final XDOM xdom;

    /**
     * @see #WikiUIExtension
     */
    private final Syntax syntax;

    /**
     * @see #WikiUIExtension
     */
    private final Map<String, String> parameters;

    /**
     * @see #WikiUIExtension
     */
    private VelocityManager velocityManager;

    /**
     * Used to transform the macros within the extension content.
     */
    private Transformation macroTransformation;

    /**
     * The execution context, used to access to the {@link com.xpn.xwiki.XWikiContext}.
     */
    private Execution execution;

    /**
     * Default constructor.
     *
     * @param objectReference the reference of the object holding this extension
     * @param name the name of the extension
     * @param extensionPointId ID of the extension point this extension is designed for
     * @param xdom the XDOM to be rendered when this extension is displayed
     * @param syntax the Syntax of the extension XDOM
     * @param parameters the extension parameters map
     * @param componentManager the XWiki component manager
     */
    public WikiUIExtension(ObjectReference objectReference, String name, String extensionPointId,
        XDOM xdom, Syntax syntax, Map<String, String> parameters, ComponentManager componentManager)
    {
        this.documentReference = (DocumentReference) objectReference.getParent();
        this.name = name;
        this.extensionPointId = extensionPointId;
        this.xdom = xdom;
        this.syntax = syntax;
        this.parameters = parameters;
        try {
            this.macroTransformation = componentManager.<Transformation>getInstance(Transformation.class, "macro");
            this.execution = componentManager.getInstance(Execution.class);
            this.velocityManager = componentManager.getInstance(VelocityManager.class);
            EntityReferenceSerializer<String> serializer =
                componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
            this.roleHint = serializer.serialize(objectReference);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to get an instance for a component role required by Wiki Components. "
                + "Error: [%s]", e.getMessage()));
        }
    }

    @Override
    public String getId()
    {
        return this.name;
    }

    @Override
    public String getExtensionPointId()
    {
        return this.extensionPointId;
    }

    @Override
    public Map<String, String> getParameters()
    {
        Map<String, String> result = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
            StringWriter writer = new StringWriter();
            try {
                this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                    "", entry.getValue());
                result.put(entry.getKey(), writer.toString());
            } catch (Exception e) {
                LOGGER.warn(String.format(
                    "Failed to evaluate UI extension data value, key [%s], value [%s]. Reason: [%s]",
                    entry.getKey(), entry.getValue(), e.getMessage()));
            }

        }

        return result;
    }

    @Override
    public Block execute()
    {
        // We need to clone the xdom to avoid transforming the original and make it useless after the first
        // transformation
        XDOM transformedXDOM = xdom.clone();

        // Perform macro transformations.
        try {
            TransformationContext transformationContext = new TransformationContext(xdom, syntax);
            transformationContext.setId(this.getRoleHint());
            macroTransformation.transform(transformedXDOM, transformationContext);
        } catch (Exception e) {
            LOGGER.error("Error while executing wiki component macro transformation for extension [{}]",
                documentReference.toString());
        }

        return new CompositeBlock(transformedXDOM.getChildren());
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    @Override
    public Type getRoleType()
    {
        return UIExtension.class;
    }

    @Override
    public String getRoleHint()
    {
        return roleHint;
    }
}
