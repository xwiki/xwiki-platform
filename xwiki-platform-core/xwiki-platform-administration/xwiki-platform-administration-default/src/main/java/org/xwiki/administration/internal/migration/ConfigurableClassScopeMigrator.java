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
package org.xwiki.administration.internal.migration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Dedicated component for migrating ConfigurableClass and more specifically the old configure globally property to
 * scope property.
 *
 * @version $Id$
 * @since 13.6RC1
 */
@Component(roles = ConfigurableClassScopeMigrator.class)
@Singleton
public class ConfigurableClassScopeMigrator
{
    private static final LocalDocumentReference CONFIGURABLECLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "ConfigurableClass");
    private static final String OLD_CONFIGURE_GLOBALLY_PROPERTY = "configureGlobally";
    private static final String NEW_SCOPE_PROPERTY = "scope";
    private static final String WIKI_SCOPE = "WIKI";
    private static final String SPACE_SCOPE = "SPACE";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Search for all documents with a  ConfigurableClass xobject that contains the old property to migrate them, and
     * perform the migration.
     *
     * @throws QueryException in case of error to search for the pages containing the xobjects to migrate.
     * @throws XWikiException in case of error during the migration itself.
     */
    public void migrateAllConfigurableClass() throws QueryException, XWikiException
    {
        String statement = String.format(", BaseObject as obj, IntegerProperty as prop where doc.fullName = obj.name "
            + "and obj.className = 'XWiki.ConfigurableClass' and obj.id=prop.id.id and prop.id.name='%s' "
            + "group by doc.fullName",
            OLD_CONFIGURE_GLOBALLY_PROPERTY);

        Query query = this.queryManager.createQuery(statement, Query.HQL);
        List<String> docNames = query.execute();
        if (!docNames.isEmpty()) {
            WikiReference wikiReference = this.contextProvider.get().getWikiReference();
            logger.info("[{}] documents found needing a ConfigurableClass migration for the new scope property",
                docNames.size());
            int totalCounter = 0;
            int migrationCounter = 0;
            for (String docName : docNames) {
                if (this.migrateDocument(this.documentReferenceResolver.resolve(docName, wikiReference))) {
                    migrationCounter++;
                }
                totalCounter++;
                if (totalCounter % 100 == 0) {
                    logger.info("[{}] ConfigurableClass migrations handled on [{}]", totalCounter, docNames.size());
                }
            }
            logger.info("[{}] ConfigurableClass documents have been saved on [{}] documents needing migration",
                migrationCounter, docNames.size());
        }
    }

    private boolean migrateDocument(DocumentReference documentReference) throws XWikiException
    {
        boolean modified = false;
        XWikiContext context = this.contextProvider.get();
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);
        List<BaseObject> xObjects = document.getXObjects(CONFIGURABLECLASS_REFERENCE);
        for (BaseObject xObject : xObjects) {
            int oldProperty = xObject.getIntValue(OLD_CONFIGURE_GLOBALLY_PROPERTY, -1);
            boolean modifiedObject = false;
            if (oldProperty == 0) {
                xObject.set(NEW_SCOPE_PROPERTY, SPACE_SCOPE, context);
                modifiedObject = true;
            } else if (oldProperty == 1) {
                xObject.set(NEW_SCOPE_PROPERTY, WIKI_SCOPE, context);
                modifiedObject = true;
            }
            if (modifiedObject) {
                xObject.removeField(OLD_CONFIGURE_GLOBALLY_PROPERTY);
                modified = true;
            }
        }
        if (modified) {
            context.getWiki()
                .saveDocument(document, "Migrate ConfigurableClass to use scope property", true, context);
        }
        return modified;
    }
}
