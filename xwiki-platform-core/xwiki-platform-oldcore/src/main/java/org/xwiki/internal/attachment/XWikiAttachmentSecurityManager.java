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
package org.xwiki.internal.attachment;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.web.DownloadAction;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * Generic check to define if an attachment can be provided inline or if it needs to be downloaded.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component(roles = XWikiAttachmentSecurityManager.class)
@Singleton
public class XWikiAttachmentSecurityManager
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Container container;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Check if an attachment should be downloaded or can be displayed inline.
     * Attachments should be downloaded if:
     * <ul>
     *     <li>the request contains a force-download parameter with the value 1</li>
     *     <li>or the mimetype is part of the forceDownload property list</li>
     *     <li>or no custom whitelist exists and a custom blacklist exists and contains the mimetype and the attachment
     *     has not been added by a user with programming rights</li>
     *     <li>or the whitelist (default or custom) does not contains the mimetype and the attachment has not been added
     *     by a user with programming rights</li>
     * </ul>
     *
     * Note that in the case of the request does not contain a force-downloaded parameter with the value 1 and the
     * mimetype is not part of the forceDownload property list, but the attachment has been added by a user with
     * programming right, then the attachment is always displayed inline.
     *
     * @param attachment the attachment to check if it should be downloaded or provided inline.
     * @return {@code true} if the attachment should be downloaded, and {@code false} if it can be displayed inline.
     */
    public boolean shouldBeDownloaded(XWikiAttachment attachment)
    {
        DocumentReference authorReference = attachment.getAuthorReference();
        boolean hasPR = this.authorizationManager.hasAccess(Right.PROGRAM, authorReference,
            this.contextProvider.get().getWikiReference());
        String mimeType = attachment.getMimeType(this.contextProvider.get());
        return shouldBeDownloaded(mimeType, hasPR);
    }

    private boolean shouldBeDownloaded(String mimeType, boolean hasPR)
    {
        boolean result;
        Request request = this.container.getRequest();
        boolean whiteListExists = configuration.containsKey(DownloadAction.WHITELIST_PROPERTY);
        boolean blackListExists = configuration.containsKey(DownloadAction.BLACKLIST_PROPERTY);
        List<String> blackList = configuration.getProperty(DownloadAction.BLACKLIST_PROPERTY, Collections.emptyList());
        List<String> whiteList =
            configuration.getProperty(DownloadAction.WHITELIST_PROPERTY, DownloadAction.MIMETYPE_WHITELIST);
        List<String> forceDownloadList =
            configuration.getProperty(DownloadAction.FORCE_DOWNLOAD_PROPERTY, Collections.emptyList());

        if ("1".equals(request.getParameter("force-download")) || forceDownloadList.contains(mimeType)) {
            result = true;
        } else if (hasPR) {
            result = false;
        } else if (blackListExists && !whiteListExists) {
            result = blackList.contains(mimeType);
        } else {
            result = !whiteList.contains(mimeType);
        }
        return result;
    }

    /**
     * Check if a resource with a given mimetype should be downloaded or can be displayed inline.
     * Attachments should be downloaded if:
     * <ul>
     *     <li>the request contains a force-download parameter with the value 1</li>
     *     <li>or the mimetype is part of the forceDownload property list</li>
     *     <li>or no custom whitelist exists and a custom blacklist exists and contains the mimetype</li>
     *     <li>or the whitelist (default or custom) does not contains the mimetype</li>
     * </ul>
     *
     * @param mimeType the mimetype of the resource to check if it should be downloaded or provided inline.
     * @return {@code true} if the resource should be downloaded, and {@code false} if it can be displayed inline.
     */
    public boolean shouldBeDownloaded(String mimeType)
    {
        return shouldBeDownloaded(mimeType, false);
    }
}
