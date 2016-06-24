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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.edit.Editor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Discovers editors defined in wiki pages using {@code XWiki.EditorClass} objects.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Named("wiki")
@Singleton
public class WikiEditorSource implements EditorSource
{
    /**
     * The name of the {@code dataType} property of the XWiki.EditorClass.
     */
    private static final String DATA_TYPE = "dataType";

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public <D> List<Editor<D>> getEditors(Type dataType)
    {
        try {
            Query query =
                this.queryManager.createQuery("where doc.object(XWiki.EditorClass).dataType = :dataType", Query.XWQL);
            query.bindValue(DATA_TYPE, dataType.getTypeName());
            List<Editor<D>> editors = new ArrayList<>();
            for (Object result : query.execute()) {
                Editor<D> editor = getEditor(result);
                if (editor != null) {
                    editors.add(editor);
                }
            }
            return editors;
        } catch (QueryException e) {
            this.logger.warn("Failed to query the [{}] editors defined in wiki pages. Root cause: [{}]", dataType,
                ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }

    @Override
    public <D> Editor<D> getEditor(Type dataType, String hint)
    {
        try {
            Query query =
                this.queryManager.createQuery("from doc.object(XWiki.EditorClass) as editor "
                    + "where editor.dataType = :dataType and editor.roleHint = :roleHint", Query.XWQL);
            query.bindValue(DATA_TYPE, dataType.getTypeName());
            query.bindValue("roleHint", hint);
            for (Object result : query.execute()) {
                Editor<D> editor = getEditor(result);
                if (editor != null) {
                    return editor;
                }
            }
        } catch (QueryException e) {
            this.logger.warn("Failed to query the [{}] editors with [{}] role hint defined in wiki pages. "
                + "Root cause: [{}]", dataType, hint, ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    private <D> Editor<D> getEditor(Object docFullName)
    {
        DocumentReference editorReference = this.currentDocumentReferenceResolver.resolve(docFullName.toString());
        if (this.authorizationManager.hasAccess(Right.VIEW, editorReference)) {
            try {
                return new WikiEditor<>(editorReference, this.componentManagerProvider.get());
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to create editor from document [{}]. Root cause: [{}]", editorReference,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return null;
    }
}
