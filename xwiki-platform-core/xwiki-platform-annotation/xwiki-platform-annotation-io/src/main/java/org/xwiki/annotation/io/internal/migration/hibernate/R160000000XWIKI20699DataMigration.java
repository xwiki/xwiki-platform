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
package org.xwiki.annotation.io.internal.migration.hibernate;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.internal.migration.AbstractDocumentsMigration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Select and queue the document that can require an annotation target cleanup. Self-referencing annotation targets are
 * not necessary and can get outdated in case of page move/copy. Therefore, it is better to replace them with the empty
 * string, implicitly targeting the document holding the annotation XObject.
 *
 * @version $Id$
 * @since 15.5.5
 * @since 15.10.1
 * @since 16.0RC1
 */
@Component
@Named("R160000000XWIKI20699")
@Singleton
public class R160000000XWIKI20699DataMigration extends AbstractDocumentsMigration
{
    @Override
    public String getDescription()
    {
        return "Queue documents containing annotations with a non-empty target for cleanup";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(160000000);
    }

    @Override
    protected String getTaskType()
    {
        return AnnotationInternalTargetFixTaskConsumer.ID;
    }

    @Override
    protected List<DocumentReference> selectDocuments() throws DataMigrationException
    {
        XWikiContext context = getXWikiContext();
        XWiki wiki = getXWikiContext().getWiki();

        try {
            return wiki.getStore().getQueryManager()
                .createQuery("SELECT distinct doc.fullName "
                    + "FROM XWikiDocument doc, BaseObject as obj, StringProperty as prop "
                    + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiComments' "
                    + "and obj.id = prop.id.id "
                    + "and prop.id.name='target'"
                    + "and prop.value <> ''", Query.HQL)
                .setWiki(context.getWikiId())
                .execute()
                .stream()
                .flatMap(fullName -> resolveDocumentReference(String.valueOf(fullName), null).stream())
                .collect(Collectors.toList());
        } catch (QueryException e) {
            throw new DataMigrationException(
                String.format("Failed retrieve the list of all the documents with annotations for wiki [%s].",
                    wiki.getName()), e);
        }
    }
}
