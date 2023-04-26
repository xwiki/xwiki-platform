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
package org.xwiki.search.solr.internal;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Initializes the configuration page for the indexing user.
 *
 * @version $Id$
 * @since 14.8M1
 */
@Component
@Named("XWiki.SolrSearchAdminIndexingUser")
@Singleton
// initalize after the corresponding class
@Priority(ComponentDescriptor.DEFAULT_PRIORITY + 100)
public class IndexingUserConfigurationInitializer extends AbstractMandatoryDocumentInitializer
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Constructor setting the page reference.
     */
    public IndexingUserConfigurationInitializer()
    {
        super(DefaultIndexingUserConfig.CONFIG_PAGE);
    }

    /**
     * Configuration is only needed in the main wiki.
     * @return always true
     */
    @Override
    public boolean isMainWikiOnly()
    {
        return true;
    }

    @Override
    protected boolean updateDocumentFields(XWikiDocument document, String title)
    {
        boolean needsUpdate = super.updateDocumentFields(document, title);
        XWikiContext context = contextProvider.get();

        if (document.getXObject(DefaultIndexingUserConfig.CONFIG_CLASS) == null) {
            try {
                document.newXObject(DefaultIndexingUserConfig.CONFIG_CLASS, context);
                needsUpdate = true;
            } catch (XWikiException e) {
                // using String.format here so the exception is logged with stack trace
                logger.error(String.format("Error adding the [%s] object to the document [%s]",
                    DefaultIndexingUserConfig.CONFIG_CLASS, document.getDocumentReference()), e);
            }
        }
        return needsUpdate;
    }
}
