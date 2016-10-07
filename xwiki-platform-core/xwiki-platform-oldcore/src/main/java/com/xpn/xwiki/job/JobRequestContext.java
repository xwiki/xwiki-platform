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
package com.xpn.xwiki.job;

import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Contains various information about the context which asked for a job execution.
 * 
 * @version $Id$
 * @since 8.3RC1
 */
public class JobRequestContext
{
    /**
     * The key to user in the {@link Request} properties map.
     */
    public static final String KEY = "oldcore.xwikicontext";

    private boolean wikiIdSet;

    private String wikiId;

    private boolean documentSet;

    private XWikiDocument document;

    private boolean sDocumentSet;

    private XWikiDocument sDocument;

    private boolean userReferenceSet;

    private DocumentReference userReference;

    /**
     * Default constructor.
     */
    public JobRequestContext()
    {
    }

    /**
     * @param xcontext the XWiki context to extract informations from
     */
    public JobRequestContext(XWikiContext xcontext)
    {
        if (xcontext != null) {
            setWikiId(xcontext.getWikiId());
            setUserReference(xcontext.getUserReference());
            setDocument(xcontext.getDoc());
            setSDocument((XWikiDocument) xcontext.get(XWikiDocument.CKEY_SDOC));
        }
    }

    /**
     * @return true if the identifier of the wiki has been set
     */
    public boolean isWikiIdSet()
    {
        return this.wikiIdSet;
    }

    /**
     * @return the identifier of the wiki
     */
    public String getWikiId()
    {
        return this.wikiId;
    }

    /**
     * @param wikiId the identifier of the wiki
     */
    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
        this.wikiIdSet = true;
    }

    /**
     * @return true if the reference of the user has been set
     */
    public boolean isUserReferenceSet()
    {
        return this.userReferenceSet;
    }

    /**
     * @return the reference of the user
     */
    public DocumentReference getUserReference()
    {
        return this.userReference;
    }

    /**
     * @param userReference the reference of the user
     */
    public void setUserReference(DocumentReference userReference)
    {
        this.userReference = userReference;
        this.userReferenceSet = true;
    }

    /**
     * @return true of the current document has been set
     */
    public boolean isDocumentSet()
    {
        return this.documentSet;
    }

    /**
     * @param document the current document
     */
    public void setDocument(XWikiDocument document)
    {
        this.document = document;
        this.documentSet = true;
    }

    /**
     * @return the current document
     */
    public XWikiDocument getDocument()
    {
        return this.document;
    }

    /**
     * @return true if the document holding the current author has been set
     */
    public boolean isSDocumentSet()
    {
        return this.sDocumentSet;
    }

    /**
     * @param sdocument the document holding the current author
     */
    public void setSDocument(XWikiDocument sdocument)
    {
        this.sDocument = sdocument;
        this.sDocumentSet = true;
    }

    /**
     * @return the document holding the current author
     */
    public XWikiDocument getSDocument()
    {
        return this.sDocument;
    }
}
