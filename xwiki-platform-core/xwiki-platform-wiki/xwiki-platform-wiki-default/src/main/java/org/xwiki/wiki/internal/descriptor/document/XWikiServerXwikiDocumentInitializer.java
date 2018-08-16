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
package org.xwiki.wiki.internal.descriptor.document;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Initialize main wiki descriptor with what come from the first request.
 * 
 * @version $Id$
 * @since 8.0RC1
 */
@Component
@Named("XWiki.XWikiServerXwiki")
@Singleton
public class XWikiServerXwikiDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "XWikiServerXwiki";

    /**
     * Used to access the XWiki model.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    private Container container;

    /**
     * Default constructor.
     */
    public XWikiServerXwikiDocumentInitializer()
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, DOCUMENT_NAME));
    }

    @Override
    public boolean isMainWikiOnly()
    {
        // Initialize it only for the main wiki.
        return true;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Add a descriptor if none already exist
        if (document.getXObject(XWikiServerClassDocumentInitializer.SERVER_CLASS) == null) {
            XWikiContext xcontext = this.contextProvider.get();

            try {
                BaseObject xobject = document.newXObject(XWikiServerClassDocumentInitializer.SERVER_CLASS, xcontext);

                xobject.setLargeStringValue(XWikiServerClassDocumentInitializer.FIELD_DESCRIPTION, "Main wiki");
                xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE, "Main.WebHome");
                xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_LANGUAGE, "en");
                xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_STATE, "active");
                xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_VISIBILITY, "public");
                xobject.setLargeStringValue(XWikiServerClassDocumentInitializer.FIELD_OWNER,
                    XWikiRightService.SUPERADMIN_USER_FULLNAME);
                xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME, "Home");

                // Initialize the alias and the protocol with the input URL
                Request request = this.container.getRequest();
                if (request instanceof ServletRequest) {
                    ServletRequest servletRequest = (ServletRequest) request;
                    URL sourceURL = HttpServletUtils.getSourceBaseURL(servletRequest.getHttpServletRequest());
                    xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER, sourceURL.getHost());
                    if (sourceURL.getProtocol().equals("https")) {
                        // Explicitly set the secure property if the input is HTTPS
                        xobject.setIntValue(XWikiServerClassDocumentInitializer.FIELD_SECURE, 1);
                    }
                    if (sourceURL.getPort() != -1) {
                        xobject.setIntValue(XWikiServerClassDocumentInitializer.FIELD_PORT, sourceURL.getPort());
                    }
                } else {
                    xobject.setStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER, "localhost");
                    xobject.setIntValue(XWikiServerClassDocumentInitializer.FIELD_SECURE, 0);
                }

                needsUpdate = true;
            } catch (XWikiException e) {
                this.logger.error("Faied to initialize main wiki descriptor", e);
            }
        }

        return needsUpdate;
    }
}
