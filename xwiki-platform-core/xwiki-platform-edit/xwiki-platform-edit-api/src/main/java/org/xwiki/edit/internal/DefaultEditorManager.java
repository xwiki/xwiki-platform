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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;

/**
 * Default {@link EditorManager} implementation.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
public class DefaultEditorManager implements EditorManager
{
    @Inject
    private EditConfiguration configuration;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public <D> List<Editor<D>> getEditors(Type dataType)
    {
        DefaultParameterizedType editorType = new DefaultParameterizedType(null, Editor.class, dataType);
        try {
            return this.componentManagerProvider.get().getInstanceList(editorType);
        } catch (ComponentLookupException e) {
            // No editors found.
            return Collections.emptyList();
        }
    }

    @Override
    public <D> List<Editor<D>> getEditors(Type dataType, String category)
    {
        List<Editor<D>> editors = new ArrayList<>();
        for (Editor<D> editor : this.<D>getEditors(dataType)) {
            if (Objects.equals(category, editor.getDescriptor().getCategory())) {
                editors.add(editor);
            }
        }
        return editors;
    }

    @Override
    public <D> Editor<D> getEditor(Type dataType, String hint)
    {
        DefaultParameterizedType editorType = new DefaultParameterizedType(null, Editor.class, dataType);
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (componentManager.hasComponent(editorType, hint)) {
            try {
                return componentManager.getInstance(editorType, hint);
            } catch (ComponentLookupException e) {
                throw new RuntimeException(String.format("Failed to look up the [%s] editor with hint [%s]",
                    dataType.getTypeName(), hint), e);
            }
        } else {
            // No such editor component found.
            return null;
        }
    }

    @Override
    public <D> Editor<D> getDefaultEditor(Type dataType)
    {
        return getDefaultEditor(dataType, null);
    }

    @Override
    public <D> Editor<D> getDefaultEditor(Type dataType, String category)
    {
        Editor<D> defaultEditor;
        String defaultEditorHint = this.configuration.getDefaultEditor(dataType, category);
        if (defaultEditorHint == null) {
            // There's no editor configured for the specified data type and category. See if there is any editor
            // available in the specified category. This way we don't force the user to configure the default editor for
            // each category when there's only one editor available per category.
            List<Editor<D>> editors = getEditors(dataType, category);
            if (editors.isEmpty()) {
                // See if there is any editor available for the specified data type.
                editors = getEditors(dataType);
            }
            defaultEditor = editors.isEmpty() ? null : editors.get(0);
        } else {
            defaultEditor = getEditorWithFallBackOnCategory(dataType, defaultEditorHint);
        }
        return defaultEditor;
    }

    private <D> Editor<D> getEditorWithFallBackOnCategory(Type dataType, String roleHint)
    {
        Editor<D> editor = getEditor(dataType, roleHint);
        if (editor == null) {
            // Consider the roleHint to be a category of editors and return the default one.
            editor = getDefaultEditor(dataType, roleHint);
        }
        return editor;
    }
}
