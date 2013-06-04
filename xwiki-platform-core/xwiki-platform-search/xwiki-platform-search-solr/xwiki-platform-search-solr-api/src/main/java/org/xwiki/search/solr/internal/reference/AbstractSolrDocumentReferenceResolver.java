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
package org.xwiki.search.solr.internal.reference;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Base implementation.
 * 
 * @version $Id$
 * @since 5.1M2
 */
public abstract class AbstractSolrDocumentReferenceResolver implements SolrDocumentReferenceResolver
{
    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * The logger.
     */
    @Inject
    protected Logger logger;

    /**
     * Utility method.
     * 
     * @param documentReference reference to a document.
     * @return the {@link XWikiDocument} instance referenced.
     * @throws Exception if problems occur.
     */
    protected XWikiDocument getDocument(DocumentReference documentReference) throws Exception
    {
        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);

        return document;
    }
}
