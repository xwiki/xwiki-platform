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
package org.xwiki.index.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Update the links indexing of a document.
 *
 * @version $Id$
 * @since 14.2RC1
 * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
 */
@Component
@Singleton
@Named(DefaultLinksTaskConsumer.LINKS_TASK_TYPE)
@Deprecated(since = "14.8RC1")
public class DefaultLinksTaskConsumer implements TaskConsumer
{
    /**
     * Identifier of the links tasks type.
     */
    public static final String LINKS_TASK_TYPE = "links";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Logger logger;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        XWikiContext context = this.xcontextProvider.get();

        // Skip if backlinks are not supported. Note that this task is not supposed to be queued when backlinks are
        // not supported.
        if (context.getWiki().hasBacklinks(context)) {
            try {
                XWikiDocument doc = this.documentRevisionProvider.getRevision(documentReference, version);
                // Verify that the document has not been removed in the meantime.
                if (doc != null) {
                    context.getWiki().getHibernateStore().saveLinks(doc, context, true);
                }
            } catch (XWikiException e) {
                throw new IndexException(String.format("Failed to updated links for document [%s] version [%s].",
                    documentReference, version), e);
            }
        } else {
            this.logger.warn(
                "Skipping for document [{}] version [{}] because backlinks are not supported in this wiki.",
                documentReference, version);
        }
    }
}
