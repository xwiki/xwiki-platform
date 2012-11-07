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
import org.xwiki.component.wiki.WikiComponentScope;
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
    private final DocumentReference authorReference;

    /**
     * @see #WikiUIExtension
     */
    private final String id;

    /**
     * @see #WikiUIExtension
     */
    private final String extensionPointId;

    /**
     * @see #WikiUIExtension
     */
    private String roleHint;

    /**
     * @see #setXDOM(org.xwiki.rendering.block.XDOM)
     */
    private XDOM xdom;

    /**
     * @see #setSyntax(org.xwiki.rendering.syntax.Syntax)
     */
    private Syntax syntax;

    /**
     * @see #setParameters(java.util.Map)
     */
    private Map<String, String> parameters;

    /**
     * @see #setScope(org.xwiki.component.wiki.WikiComponentScope)
     */
    private WikiComponentScope scope;

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
     * @param id the id of the extension
     * @param extensionPointId ID of the extension point this extension is designed for
     * @param objectReference the reference of the object holding this extension
     * @param authorReference the reference of the author of the document holding this extension
     * @param cm the XWiki component manager
     */
    public WikiUIExtension(String id, String extensionPointId, ObjectReference objectReference,
        DocumentReference authorReference, ComponentManager cm)
    {
        this.documentReference = (DocumentReference) objectReference.getParent();
        this.authorReference = authorReference;
        this.id = id;
        this.extensionPointId = extensionPointId;
        try {
            this.macroTransformation = cm.<Transformation>getInstance(Transformation.class, "macro");
            this.execution = cm.getInstance(Execution.class);
            this.velocityManager = cm.getInstance(VelocityManager.class);
            EntityReferenceSerializer<String> serializer = cm.getInstance(EntityReferenceSerializer.TYPE_STRING);
            this.roleHint = serializer.serialize(objectReference);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to get an instance for a component role required by Wiki Components. "
                + "Error: [%s]", e.getMessage()));
        }
    }


    /**
     * Set the XDOM to be rendered when this extension is displayed.
     *
     * @param xdom the XDOM to be rendered when this extension is displayed
     */
    public void setXDOM(XDOM xdom)
    {
        this.xdom = xdom;
    }

    /**
     * Set the Syntax in which the extension XDOM is written.
     *
     * @param syntax the Syntax in which the extension XDOM is written.
     */
    public void setSyntax(Syntax syntax)
    {
        this.syntax = syntax;
    }

    /**
     * Set the extension parameters.
     *
     * @param parameters the extension parameters
     */
    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    /**
     * Set the scope of the extension.
     *
     * @param scope the scope of the extension
     */
    public void setScope(WikiComponentScope scope)
    {
        this.scope = scope;
    }

    @Override
    public String getId()
    {
        return this.id;
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
    public DocumentReference getAuthorReference()
    {
        return authorReference;
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

    @Override
    public WikiComponentScope getScope()
    {
        return scope;
    }
}
