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

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.context.concurrent.ContextStore;
import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Contains various information about the context which asked for a job execution.
 * 
 * @version $Id$
 * @since 8.3RC1
 * @deprecated since 10.9RC1, use {@link ContextStore} instead
 */
@Deprecated
public class JobRequestContext implements Serializable
{
    /**
     * The key to user in the {@link Request} properties map.
     */
    public static final String KEY = "oldcore.xwikicontext";

    private static final long serialVersionUID = 1L;

    private boolean wikiIdSet;

    private String wikiId;

    private boolean documentSet;

    private DocumentReference documentReference;

    private transient XWikiDocument document;

    private boolean sDocumentSet;

    private DocumentReference sDocumentReference;

    private transient XWikiDocument sDocument;

    private boolean userReferenceSet;

    private DocumentReference userReference;

    private boolean requestSet;

    private URL requestURL;

    private String requestContextPath;

    private Map<String, String[]> requestParameters;

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

            XWikiRequest request = xcontext.getRequest();
            if (request != null) {
                if (request.getRequestURL() != null) {
                    setRequestUrl(HttpServletUtils.getSourceURL(request));
                    setRequestContextPath(request.getContextPath());
                }
                if (request.getParameterMap() != null) {
                    setRequestParameters(request.getParameterMap());
                }
            }
        }
    }

    /**
     * Register part of the {@link XWikiContext} in the job request.
     * 
     * @param request the job request
     * @param xcontext the XWiki context
     * @since 8.4RC1
     */
    public static void set(AbstractRequest request, XWikiContext xcontext)
    {
        if (xcontext != null) {
            request.setProperty(JobRequestContext.KEY, new JobRequestContext(xcontext));
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
     * @param documentReference the reference of the current document
     * @since 9.7
     */
    public void setDocumentReference(DocumentReference documentReference)
    {
        if (!Objects.equals(documentReference, this.documentReference)) {
            this.documentReference = documentReference;

            this.document = null;
            this.documentSet = true;
        }
    }

    /**
     * @return the reference of the current document
     * @since 9.7
     */
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    /**
     * @param document the current document
     */
    public void setDocument(XWikiDocument document)
    {
        this.document = document;
        this.documentSet = true;

        this.documentReference = document != null ? document.getDocumentReferenceWithLocale() : null;
    }

    /**
     * @return true if the document holding the current author has been set
     */
    public boolean isSDocumentSet()
    {
        return this.sDocumentSet;
    }

    /**
     * @return the current document
     */
    public XWikiDocument getDocument()
    {
        return this.document;
    }

    /**
     * @param sdocumentReference the reference of the document holding the current author
     * @since 9.7
     */
    public void setSDocumentReference(DocumentReference sdocumentReference)
    {
        if (!Objects.equals(sdocumentReference, this.sDocumentReference)) {
            this.sDocumentReference = sdocumentReference;

            this.sDocument = null;
            this.sDocumentSet = true;
        }
    }

    /**
     * @return the reference of the document holding the current author
     * @since 9.7
     */
    public DocumentReference getSDocumentReference()
    {
        return sDocumentReference;
    }

    /**
     * @param sdocument the document holding the current author
     */
    public void setSDocument(XWikiDocument sdocument)
    {
        this.sDocument = sdocument;
        this.sDocumentSet = true;

        this.sDocumentReference = sdocument != null ? sdocument.getDocumentReferenceWithLocale() : null;
    }

    /**
     * @return the document holding the current author
     */
    public XWikiDocument getSDocument()
    {
        return this.sDocument;
    }

    /**
     * @return true if the request informations have been set
     * @since 8.4RC1
     */
    public boolean isRequestSet()
    {
        return this.requestSet;
    }

    /**
     * @param requestURL the request {@link URL}
     * @since 8.4RC1
     */
    public void setRequestUrl(URL requestURL)
    {
        this.requestURL = requestURL;
        this.requestSet = true;
    }

    /**
     * @return the request URL
     * @since 8.4RC1
     */
    public URL getRequestURL()
    {
        return this.requestURL;
    }

    /**
     * @return the requestContextPath
     * @since 10.11.1
     * @since 11.0RC1
     */
    public String getRequestContextPath()
    {
        return this.requestContextPath;
    }

    /**
     * @param requestContextPath the requestContextPath to set
     * @since 10.11.1
     * @since 11.0RC1
     */
    public void setRequestContextPath(String requestContextPath)
    {
        this.requestContextPath = requestContextPath;
    }

    /**
     * @param requestParameters the parameters of the request
     * @since 8.4RC1
     */
    public void setRequestParameters(Map<String, String[]> requestParameters)
    {
        this.requestParameters = new HashMap<>(requestParameters);
    }

    /**
     * @return the parameters of the request
     * @since 8.4RC1
     */
    public Map<String, String[]> getRequestParameters()
    {
        return this.requestParameters;
    }
}
