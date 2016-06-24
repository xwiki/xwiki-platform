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

import javax.inject.Provider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.edit.AbstractEditor;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.script.ScriptContextManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Represents an {@link Editor} defined in a wiki page using the {@code XWiki.EditorClass} object.
 *
 * @param <D> the type of data this editor can edit
 * @version $Id$
 * @since 8.2RC1
 */
public class WikiEditor<D> extends AbstractEditor<D>
{
    private static final LocalDocumentReference EDITOR_CLASS_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE,
        "EditorClass");

    private final DocumentReference editorReference;

    private final ComponentManager componentManager;

    /**
     * Creates a new editor defined in the specified wiki page.
     * 
     * @param editorReference the reference of the document that defines the editor
     * @param componentManager the component manager
     * @throws ComponentLookupException if the required components are missing
     */
    public WikiEditor(DocumentReference editorReference, ComponentManager componentManager)
        throws ComponentLookupException
    {
        super(componentManager.getInstance(ScriptContextManager.class));
        this.editorReference = editorReference;
        this.componentManager = componentManager;
    }

    @Override
    public EditorDescriptor getDescriptor()
    {
        try {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            XWikiContext xcontext = xcontextProvider.get();
            BaseObject editorObject =
                xcontext.getWiki().getDocument(this.editorReference, xcontext).getXObject(EDITOR_CLASS_REFERENCE);
            return new WikiEditorDescriptor(editorObject);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String render() throws EditException
    {
        try {
            Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
            XWikiContext xcontext = xcontextProvider.get();
            return xcontext.getWiki().getDocument(this.editorReference, xcontext).getRenderedContent(xcontext);
        } catch (Exception e) {
            throw new EditException("Failed to render the editor code.", e);
        }
    }
}
