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
package org.xwiki.edit.internal;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.edit.AbstractEditor;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * An {@link Editor} implemented as a {@link WikiComponent}.
 * 
 * @param <D> the type of data that can be edited by this editor
 * @version $Id$
 * @since 8.2RC1
 */
@Component(roles = EditorWikiComponent.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class EditorWikiComponent<D> extends AbstractEditor<D> implements WikiComponent
{
    private static final LocalDocumentReference EDITOR_CLASS_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE,
        "EditorClass");

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private DocumentReference authorReference;

    private DocumentReference editorReference;

    private Type roleType;

    private WikiComponentScope scope;

    private EditorDescriptor descriptor;

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.editorReference;
    }

    @Override
    public String getRoleHint()
    {
        return getDescriptor().getId();
    }

    @Override
    public Type getRoleType()
    {
        return this.roleType;
    }

    @Override
    public WikiComponentScope getScope()
    {
        return this.scope;
    }

    @Override
    public EditorDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    @Override
    protected String render() throws EditException
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            return xcontext.getWiki().getDocument(this.getDocumentReference(), xcontext).getRenderedContent(xcontext);
        } catch (Exception e) {
            throw new EditException("Failed to render the editor code.", e);
        }
    }

    /**
     * Initializes the editor component based on the definition provided by the specified document.
     * 
     * @param editorReference the reference to the wiki page that defines the editor (i.e. that has a
     *            {@code XWiki.EditorClass} object)
     * @throws WikiComponentException if the editor component fails to be initialized
     */
    public void initialize(DocumentReference editorReference) throws WikiComponentException
    {
        if (this.editorReference != null) {
            throw new WikiComponentException("This editor component is already initialized.");
        }

        this.editorReference = editorReference;

        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            initialize(xcontext.getWiki().getDocument(editorReference, xcontext));
        } catch (XWikiException e) {
            throw new WikiComponentException("Failed to load the editor document.", e);
        }
    }

    private void initialize(XWikiDocument editorDocument) throws WikiComponentException
    {
        this.authorReference = editorDocument.getAuthorReference();
        BaseObject editorObject = editorDocument.getXObject(EDITOR_CLASS_REFERENCE);
        if (editorObject != null) {
            initialize(editorObject);
        } else {
            throw new WikiComponentException(String.format(
                "The document [%s] is missing the XWiki.EditorClass object.", editorDocument.getDocumentReference()));
        }
    }

    private void initialize(BaseObject editorObject) throws WikiComponentException
    {
        this.descriptor = new WikiEditorDescriptor(editorObject);
        this.scope = WikiComponentScope.fromString(editorObject.getStringValue("scope"));

        String dataTypeName = editorObject.getStringValue("dataType");
        try {
            Type dataType =
                ReflectionUtils.unserializeType(dataTypeName, Thread.currentThread().getContextClassLoader());
            this.roleType = new DefaultParameterizedType(null, Editor.class, dataType);
        } catch (ClassNotFoundException e) {
            throw new WikiComponentException(String.format("The [%s] data type does not exist.", dataTypeName), e);
        }
    }
}
