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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.uiextension.UIExtension;

/**
 * Represents a dynamic component instance of a UI Extension (ie a UI Extension defined in a Wiki page) that we register
 * against the Component Manager.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class WikiUIExtension implements UIExtension, WikiComponent
{
    /**
     * The key used for the UIX context in the script context.
     */
    public static final String CONTEXT_UIX_KEY = "uix";

    /**
     * The key used for the UIX document in the UIX context.
     */
    public static final String CONTEXT_UIX_DOC_KEY = "doc";

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
    private final String roleHint;

    private final AuthorExecutor authorExecutor;

    /**
     * Parameter manager for this extension.
     */
    private WikiUIExtensionParameters parameters;

    /**
     * The renderer for this extensions.
     */
    private WikiUIExtensionRenderer renderer;

    /**
     * @see #setScope(org.xwiki.component.wiki.WikiComponentScope)
     */
    private WikiComponentScope scope = WikiComponentScope.WIKI;

    /**
     * Default constructor.
     *
     * @param roleHint the role hint of the component to create
     * @param id the id of the extension
     * @param extensionPointId ID of the extension point this extension is designed for
     * @param objectReference the reference of the object holding this extension
     * @param authorReference the reference of the author of the document holding this extension
     * @param authorExecutor the executor used to execute the extension with the proper user rights
     */
    public WikiUIExtension(String roleHint, String id, String extensionPointId, ObjectReference objectReference,
        DocumentReference authorReference, AuthorExecutor authorExecutor)
    {
        this.roleHint = roleHint;
        this.id = id;
        this.extensionPointId = extensionPointId;
        this.authorReference = authorReference;
        this.documentReference = (DocumentReference) objectReference.getParent();

        this.authorExecutor = authorExecutor;
    }

    /**
     * Set the extension parameters.
     *
     * @param parameters the extension parameters
     */
    public void setParameters(WikiUIExtensionParameters parameters)
    {
        this.parameters = parameters;
    }

    /**
     * Set the extension renderer.
     *
     * @param renderer the extension renderer
     */
    public void setRenderer(WikiUIExtensionRenderer renderer)
    {
        this.renderer = renderer;
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
        if (this.parameters != null) {
            return this.parameters.get();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Block execute()
    {
        if (this.renderer != null) {
            try {
                return this.authorExecutor.call(this.renderer::execute, getAuthorReference());
            } catch (Exception e) {
                LOGGER.error("Error while executing transformation for extension [{}]", documentReference.toString());
            }
        }

        return new WordBlock("");
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
