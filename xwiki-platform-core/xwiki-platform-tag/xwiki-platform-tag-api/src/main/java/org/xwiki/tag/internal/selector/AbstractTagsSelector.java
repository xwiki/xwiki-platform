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
package org.xwiki.tag.internal.selector;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.tag.internal.TagException;
import org.xwiki.tag.internal.TagsSelector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.tag.TagPlugin;

import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Common abstract class for the implementation of {@link TagsSelector}. Provides an implementation of
 * {@link TagsSelector#getDocumentsWithTag(String, boolean)} taking into account whether the implementation can be
 * considered as safe.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
public abstract class AbstractTagsSelector implements TagsSelector
{
    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    @Named(HiddenDocumentFilter.HINT)
    protected QueryFilter hiddenDocumentQueryFilter;

    @Inject
    @Named(UniqueDocumentFilter.HINT)
    protected QueryFilter uniqueDocumentQueryFilter;

    @Inject
    protected ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Override
    public List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments, boolean caseSensitive)
        throws TagException
    {
        String hql = ", BaseObject as obj, DBStringListProperty as prop join prop.list item"
            + " where obj.className=:className and obj.name=doc.fullName and obj.id=prop.id.id and prop.id.name='tags'";
        if (caseSensitive) {
            hql += "and item = :item";
        } else {
            hql += " and lower(item)=lower(:item)";
        }

        try {
            Query query = this.contextProvider.get().getWiki().getStore().getQueryManager().createQuery(hql, Query.HQL);
            query.bindValue("className", TagPlugin.TAG_CLASS);
            query.bindValue("item", tag);
            query.addFilter(this.uniqueDocumentQueryFilter);
            if (!includeHiddenDocuments) {
                query.addFilter(this.hiddenDocumentQueryFilter);
            }

            List<String> documents;
            List<String> rows = query.execute();
            if (isUnsafe()) {
                documents = rows;
            } else {
                documents = new ArrayList<>();
                for (String documentReferenceRepresentation : rows) {
                    if (this.contextualAuthorizationManager
                        .hasAccess(VIEW, this.stringDocumentReferenceResolver.resolve(documentReferenceRepresentation)))
                    {
                        documents.add(documentReferenceRepresentation);
                    }
                }
            }
            return documents;
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to search for document with tag [%s]", tag), e);
        }
    }

    /**
     * @return {@code true} if the implementation is unsafe, and in this case skip the view right checks on returned
     *     elements.
     */
    boolean isUnsafe()
    {
        return false;
    }
}
