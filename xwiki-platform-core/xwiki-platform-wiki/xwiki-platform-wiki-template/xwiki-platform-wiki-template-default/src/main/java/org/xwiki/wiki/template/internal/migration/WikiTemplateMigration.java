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
package org.xwiki.wiki.template.internal.migration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.template.internal.WikiTemplateClassDocumentInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrator to create a WikiManager.WikiTemplateClass on every descriptor.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@Component
@Named("R54000WikiTemplateMigration")
@Singleton
public class WikiTemplateMigration extends AbstractHibernateDataMigration
{
    private static final String OLD_TEMPLATE_PROPERTY = "iswikitemplate";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getDescription()
    {
        return "http://jira.xwiki.org/browse/XWIKI-9934";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(54000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We migrate main wiki
        return wikiDescriptorManager.getCurrentWikiId().equals(wikiDescriptorManager.getMainWikiId());
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // XWiki objects
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // WikiManager.WikiTemplateClass reference
        DocumentReference templateClassReference = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                WikiTemplateClassDocumentInitializer.DOCUMENT_SPACE,
                WikiTemplateClassDocumentInitializer.DOCUMENT_NAME);

        // XWiki.XWikiServerClass reference
        DocumentReference descriptorClassReference = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                XWiki.SYSTEM_SPACE, "XWikiServerClass");

        // Superadmin reference
        DocumentReference superAdmin = new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                XWiki.SYSTEM_SPACE, "superadmin");

        try {
            // Get all the descriptor documents
            String statement = "select distinct doc.fullName "
                + "from Document doc, doc.object(XWiki.XWikiServerClass) as obj";
            Query query = queryManager.createQuery(statement, Query.XWQL);
            List<String> results = query.execute();
            for (String wikiPage : results) {
                XWikiDocument document = xwiki.getDocument(documentReferenceResolver.resolve(wikiPage), context);
                // Get the "iswikitemplate" value
                BaseObject descriptorObject = document.getXObject(descriptorClassReference);
                int isTemplate = descriptorObject.getIntValue(OLD_TEMPLATE_PROPERTY, 0);
                // We remove the deprecated property from the descriptor
                descriptorObject.removeField(OLD_TEMPLATE_PROPERTY);
                // Add the new WikiManager.WikiTemplateClass object
                BaseObject object = document.getXObject(templateClassReference, true, context);
                // The new object might already exists and have a template property already set
                isTemplate = object.getIntValue(WikiTemplateClassDocumentInitializer.FIELD_ISWIKITEMPLATE, isTemplate);
                // Set the (new) value
                object.setIntValue(WikiTemplateClassDocumentInitializer.FIELD_ISWIKITEMPLATE, isTemplate);
                // The document must have an author
                document.setAuthorReference(superAdmin);
                // Save the document
                xwiki.saveDocument(document, "[UPGRADE] Upgrade the template section.", context);
            }
        } catch (QueryException e) {
            throw new DataMigrationException("Failed to get the list of all existing descriptors.", e);
        } catch (XWikiException e) {
            throw new DataMigrationException("Failed to upgrade a wiki descriptor.", e);
        }
    }
}
