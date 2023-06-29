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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.internal.migration.AbstractDocumentsMigration;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Selects and queue the document that can require a target cleanup. Self-reference target are not required and can get
 * outdated in case of page move/copy. Therefore, it is better to remplace them with an empty target, implicitly
 * targeting the document holding the annotation XObject.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 * @since 14.10.14
 */
@Component
@Named("R150500000XWIKI20699")
@Singleton
public class R150500000XWIKI20699DataMigration extends AbstractDocumentsMigration
{
    @Override
    public String getDescription()
    {
        return "Queue documents containing annotations with a non-empty target for cleanup";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(150500000);
    }

    @Override
    protected String getTaskType()
    {
        return AnnotationInternalTargetFixTaskConsumer.ID;
    }

    @Override
    protected List<String> selectDocuments() throws DataMigrationException
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
                .execute();
        } catch (QueryException e) {
            throw new DataMigrationException(
                String.format("Failed retrieve the list of all the documents with annotations for wiki [%s].",
                    wiki.getName()), e);
        }
    }
}
