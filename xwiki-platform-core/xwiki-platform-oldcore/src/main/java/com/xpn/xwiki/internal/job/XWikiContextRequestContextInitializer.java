package com.xpn.xwiki.internal.job;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.MutableRequest;
import org.xwiki.job.Request;
import org.xwiki.job.RequestContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.job.JobRequestContext;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Save and restore well known {@link XWikiContext} entries.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
@Component
@Singleton
@Named("xwikicontext")
public class XWikiContextRequestContextInitializer implements RequestContextInitializer
{
    @Inject
    private Provider<XWikiContext> writeProvider;

    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readProvider;

    @Inject
    private Logger logger;

    @Override
    public void save(MutableRequest request)
    {
        XWikiContext xcontext = this.readProvider.get();

        if (xcontext != null) {
            request.setProperty(JobRequestContext.KEY, new JobRequestContext(xcontext));
        }
    }

    @Override
    public void save(MutableRequest request, Set<String> entries)
    {
        save(request);
    }

    @Override
    public void restore(Request request)
    {
        JobRequestContext jobRequestContext = request.getProperty(JobRequestContext.KEY);

        if (jobRequestContext != null) {
            XWikiContext xcontext = this.writeProvider.get();

            // Wiki id
            if (jobRequestContext.isWikiIdSet()) {
                xcontext.setWikiId(jobRequestContext.getWikiId());
            }

            // User
            if (jobRequestContext.isUserReferenceSet()) {
                xcontext.setUserReference(jobRequestContext.getUserReference());
            }

            // Document
            if (jobRequestContext.isDocumentSet()) {
                XWikiDocument document = getDocument(jobRequestContext, xcontext);
                xcontext.setDoc(document);
            }

            // Secure document
            if (jobRequestContext.isSDocumentSet()) {
                XWikiDocument sdocument = getSDocument(jobRequestContext, xcontext);
                xcontext.put(XWikiDocument.CKEY_SDOC, sdocument);
            }

            // Request
            if (jobRequestContext.isRequestSet()) {
                xcontext.setRequest(new XWikiServletRequestStub(jobRequestContext.getRequestURL(),
                    jobRequestContext.getRequestParameters()));
            }
        }
    }

    private XWikiDocument getDocument(JobRequestContext jobRequestContext, XWikiContext xcontext)
    {
        if (jobRequestContext.getDocument() != null) {
            return jobRequestContext.getDocument();
        } else if (jobRequestContext.getDocumentReference() != null) {
            try {
                return xcontext.getWiki().getDocument(jobRequestContext.getDocumentReference(), xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to get document with reference [{}]",
                    jobRequestContext.getDocumentReference(), e);
            }
        }

        return null;
    }

    private XWikiDocument getSDocument(JobRequestContext jobRequestContext, XWikiContext xcontext)
    {
        if (jobRequestContext.getSDocument() != null) {
            return jobRequestContext.getSDocument();
        } else if (jobRequestContext.getSDocumentReference() != null) {
            try {
                return xcontext.getWiki().getDocument(jobRequestContext.getSDocumentReference(), xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to get secure document with reference [{}]",
                    jobRequestContext.getSDocumentReference(), e);
            }
        }

        return null;
    }
}
