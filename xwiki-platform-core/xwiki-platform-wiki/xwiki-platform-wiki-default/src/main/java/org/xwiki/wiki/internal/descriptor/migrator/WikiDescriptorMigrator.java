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
package org.xwiki.wiki.internal.descriptor.migrator;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * This migrator is designed to add missing properties to existing descriptors.
 *
 * @since 5.4.3
 * @version $Id$
 */
@Component
@Named("R54300WikiDescriptorMigration")
@Singleton
public class WikiDescriptorMigrator extends AbstractHibernateDataMigration
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "https://jira.xwiki.org/browse/XWIKI-10091";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54300);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // Should execute only on the main wiki
        return wikiDescriptorManager.getCurrentWikiId().equals(wikiDescriptorManager.getMainWikiId());
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        String hql = "SELECT DISTINCT doc.fullName FROM XWikiDocument doc, BaseObject obj WHERE doc.fullName = obj.name"
            + " AND obj.className = :className AND doc.fullName <> :template AND doc.fullName NOT IN "
            + "(SELECT DISTINCT doc2.fullName FROM XWikiDocument doc2, BaseObject obj2, StringProperty propPrettyName"
            + " WHERE doc2.fullName = obj2.name AND obj2.className = :className"
            + " AND propPrettyName.id = obj2.id AND propPrettyName.name = :propertyName)";

        try {
            Query query = queryManager.createQuery(hql, Query.HQL);
            query.bindValue("className", String.format("%s.%s", XWiki.SYSTEM_SPACE,
                    XWikiServerClassDocumentInitializer.DOCUMENT_NAME));
            query.bindValue("propertyName", XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME);
            query.bindValue("template", "XWiki.XWikiServerClassTemplate");
            List<String> results = query.execute();
            for (String result : results) {
                fixDocument(result);
            }
        } catch (QueryException e) {
            logger.error("Failed to perform a query on the main wiki.", e);
        }
    }

    private void fixDocument(String documentName)
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        DocumentReference documentReference = documentReferenceResolver.resolve(documentName);
        try {
            XWikiDocument document = xwiki.getDocument(documentReference, context);
            List<BaseObject> objects =  document.getXObjects(XWikiServerClassDocumentInitializer.SERVER_CLASS);
            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                String value = obj.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME);
                if (StringUtils.isBlank(value)) {
                    obj.setStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME,
                        StringUtils.capitalize(Strings.CS.removeStart(documentReference.getName(), "XWikiServer")));
                }
            }
            xwiki.saveDocument(document, "[UPGRADE] Set a default pretty name.", context);
        } catch (XWikiException e) {
            logger.warn("Failed to get or save the wiki descriptor document [{}]. You will not see the"
                    + " corresponding wiki in the Wiki Index unless you give it a Pretty Name manually. {}",
                    documentName, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
