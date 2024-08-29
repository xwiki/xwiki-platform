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
package com.xpn.xwiki.internal.skin;

import javax.inject.Provider;

import org.xwiki.filter.input.InputSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.skin.ResourceRepository;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 6.4M1
 */
public abstract class AbstractWikiResource<R extends EntityReference, I extends InputSource> extends AbstractResource<I>
    implements WikiResource<I>
{
    protected final Provider<XWikiContext> xcontextProvider;

    protected final R reference;

    protected final DocumentReference authorReference;

    protected final DocumentReference documentReference;

    public AbstractWikiResource(String id, String path, String resourceName, ResourceRepository repository, R reference,
        DocumentReference authorReference, Provider<XWikiContext> xcontextProvider)
    {
        super(id, path, resourceName, repository);

        this.reference = reference;
        this.authorReference = authorReference;
        this.documentReference = (DocumentReference) this.reference.extractReference(EntityType.DOCUMENT);
        this.xcontextProvider = xcontextProvider;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    protected XWikiDocument getDocument() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext.getWiki().getDocument(getDocumentReference(), xcontext);
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        return getURL(getDocument());
    }

    public abstract String getURL(XWikiDocument document) throws Exception;
}
