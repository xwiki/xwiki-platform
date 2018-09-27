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
package org.xwiki.panels.internal;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.internal.AbstractUIExtension;

import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Provides a bridge between Panels defined in XObjects and {@link UIExtension}.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class PanelWikiUIExtension extends AbstractUIExtension implements WikiComponent
{
    /**
     * Serializer used to transform the panel document reference into the panel ID, for example 'Panels.Quicklinks'.
     */
    private final EntityReferenceSerializer<String> serializer;

    /**
     * @see #PanelWikiUIExtension(org.xwiki.model.reference.DocumentReference,
     *      org.xwiki.model.reference.DocumentReference, org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax, org.xwiki.component.manager.ComponentManager)
     */
    private final BaseObjectReference panelReference;

    /**
     * @see #PanelWikiUIExtension(org.xwiki.model.reference.DocumentReference,
     *      org.xwiki.model.reference.DocumentReference, org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax, org.xwiki.component.manager.ComponentManager)
     */
    private final DocumentReference authorReference;

    /**
     * @see #PanelWikiUIExtension(org.xwiki.model.reference.DocumentReference,
     *      org.xwiki.model.reference.DocumentReference, org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax, org.xwiki.component.manager.ComponentManager)
     */
    private final XDOM xdom;

    /**
     * @see #PanelWikiUIExtension(org.xwiki.model.reference.DocumentReference,
     *      org.xwiki.model.reference.DocumentReference, org.xwiki.rendering.block.XDOM,
     *      org.xwiki.rendering.syntax.Syntax, org.xwiki.component.manager.ComponentManager)
     */
    private final Syntax syntax;

    /**
     * Used to update the rendering context.
     */
    private final RenderingContext renderingContext;

    /**
     * Used to transform the macros within the extension content.
     */
    private final Transformation macroTransformation;

    private final AuthorExecutor authorExecutor;

    /**
     * Default constructor.
     *
     * @param panelReference The reference of the object in which the panel is defined
     * @param authorReference The author of the document in which the panel is defined
     * @param xdom The content to display for this panel
     * @param syntax The syntax in which the content is written
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     */
    public PanelWikiUIExtension(BaseObjectReference panelReference, DocumentReference authorReference, XDOM xdom,
        Syntax syntax, ComponentManager componentManager) throws ComponentLookupException
    {
        super(componentManager);

        this.panelReference = panelReference;
        this.authorReference = authorReference;
        this.xdom = xdom;
        this.syntax = syntax;

        this.macroTransformation = componentManager.getInstance(Transformation.class, "macro");
        this.serializer = componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.renderingContext = componentManager.getInstance(RenderingContext.class);
        this.authorExecutor = componentManager.getInstance(AuthorExecutor.class);
    }

    @Override
    public String getId()
    {
        return this.serializer.serialize(getDocumentReference());
    }

    @Override
    public String getExtensionPointId()
    {
        return "platform.panels";
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.panelReference.getDocumentReference();
    }

    @Override
    public EntityReference getEntityReference()
    {
        return this.panelReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    protected Block executeInternal() throws Exception
    {
        // We need to clone the xdom to avoid transforming the original and make it useless after the first
        // transformation
        final XDOM transformedXDOM = this.xdom.clone();

        // Perform panel transformations with the right of the panel author
        this.authorExecutor.call(() -> {
            TransformationContext transformationContext = new TransformationContext(transformedXDOM, syntax);
            transformationContext.setId(getRoleHint());
            ((MutableRenderingContext) renderingContext).transformInContext(macroTransformation, transformationContext,
                transformedXDOM);

            return null;
        }, getAuthorReference());

        return new CompositeBlock(transformedXDOM.getChildren());
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.emptyMap();
    }

    @Override
    public Type getRoleType()
    {
        return UIExtension.class;
    }

    @Override
    public String getRoleHint()
    {
        return getId();
    }

    @Override
    public WikiComponentScope getScope()
    {
        // TODO: handle scope dynamically, in the meantime it's hardcoded to "global" for backward compatibility
        return WikiComponentScope.GLOBAL;
    }
}
