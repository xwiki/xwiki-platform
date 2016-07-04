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
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.edit.Editor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Builds {@link Editor} components defined in wiki pages using {@code XWiki.EditorClass} objects.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Named("editor")
@Singleton
public class EditorWikiComponentBuilder implements WikiComponentBuilder
{
    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @SuppressWarnings("rawtypes")
    private Provider<EditorWikiComponent> editorWikiComponentProvider;

    @Override
    public List<WikiComponent> buildComponents(DocumentReference reference) throws WikiComponentException
    {
        EditorWikiComponent<?> editorWikiComponent = this.editorWikiComponentProvider.get();
        editorWikiComponent.initialize(reference);
        return Collections.singletonList(editorWikiComponent);
    }

    @Override
    public List<DocumentReference> getDocumentReferences()
    {
        List<DocumentReference> editorReferences = new ArrayList<>();
        try {
            Query query =
                this.queryManager.createQuery("from doc.object(XWiki.EditorClass) as editor where "
                    + "(editor.roleHint <> '' or (editor.roleHint is not null and '' is null))", Query.XWQL);
            for (Object result : query.execute()) {
                editorReferences.add(this.currentDocumentReferenceResolver.resolve(result.toString()));
            }
        } catch (QueryException e) {
            this.logger.warn("Failed to query the editors defined in wiki pages. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
        return editorReferences;
    }
}
