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
package org.xwiki.classloader.internal.protocol.attachmentjar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.ExtendedURLStreamHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;

/**
 * Special handler that allows building URLs that have their contents in a wiki document's attachment.
 * 
 * @version $Id$
 * @since 2.0.1
 */
@Component
@Named("attachmentjar")
@Singleton
public class AttachmentURLStreamHandler extends URLStreamHandler implements ExtendedURLStreamHandler
{
    private static final String ATTACHMENT_JAR_PROTOCOL = "attachmentjar";

    private static final String ATTACHMENT_JAR_PREFIX = ATTACHMENT_JAR_PROTOCOL + "://";

    /**
     * Create attachment name from a string reference.
     */
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    /**
     * Used to get the current document name and document URLs.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public String getProtocol()
    {
        return ATTACHMENT_JAR_PROTOCOL;
    }

    /**
     * {@inheritDoc}
     *
     * Parse the attachment URL which is in the format {@code attachmentjar://(wiki):(space).(page)@(filename)}.
     * 
     * @see URLStreamHandler#openConnection(URL)
     */
    @Override
    protected URLConnection openConnection(URL url) throws IOException
    {
        // Get the attachment reference from the URL and transform it into an AttachmentReference object
        AttachmentReference attachmentReference = this.attachmentReferenceResolver.resolve(
            getAttachmentReference(url));

        return new AttachmentURLConnection(url, attachmentReference, this.documentAccessBridge);
    }

    private String getAttachmentReference(URL url)
    {
        // If the URL doesn't start with the Attachment JAR scheme prefix something is wrong
        String urlAsString = url.toString();
        if (!urlAsString.startsWith(ATTACHMENT_JAR_PREFIX)) {
            throw new RuntimeException("An attachment JAR URL should start with ["
                + ATTACHMENT_JAR_PREFIX + "], got [" + urlAsString + "]");
        }

        String attachmentReference = urlAsString.substring(ATTACHMENT_JAR_PREFIX.length());
        try {
            // Note: we decode using UTF8 since it's the W3C recommendation.
            // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
            // TODO: Once the xwiki-url module is usable, refactor this code to use it and remove the need to
            // perform explicit decoding here.
            attachmentReference = URLDecoder.decode(attachmentReference, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
            // without that encoding.
            throw new RuntimeException("Failed to URL decode [" + attachmentReference + "] using UTF-8.", e);
        }

        return attachmentReference;
    }
}
