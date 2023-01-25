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
package org.xwiki.internal.macro.source;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @param <T> the type of the source
 * @version $Id$
 * @since 15.1RC1
 * @since 14.10.5
 */
public abstract class AbstractAttachmentMacroContentSourceLoader<T>
{
    @Inject
    private MacroContentEntitySoureConfiguration configuration;

    /**
     * @param document the document
     * @param entityReference the reference of the entity
     * @param reference the reference of the source
     * @param xcontext the XWiki context
     * @return the source
     * @throws MacroExecutionException when failing to access the entity
     */
    public T load(XWikiDocument document, EntityReference entityReference, MacroContentSourceReference reference,
        XWikiContext xcontext) throws MacroExecutionException
    {
        XWikiAttachment attachment = document.getAttachment(entityReference.getName());

        if (attachment == null) {
            throw new MacroExecutionException("Unknown attachment [" + entityReference + "]");
        }

        // Refuse to load the content of too big attachment as it could have an important impact on memory and it's very
        // unlikely to be a text file
        long attachmentSize;
        try {
            attachmentSize = attachment.getContentLongSize(xcontext);
        } catch (XWikiException e) {
            throw new MacroExecutionException("Failed to get the size of attachment [" + entityReference + "]", e);
        }
        if (attachmentSize > this.configuration.getMaximumAttachmentSize()) {
            throw new MacroExecutionException("The size of the attachment [" + entityReference + "] is too big (["
                + attachmentSize + "]) for the maximum [" + this.configuration.getMaximumAttachmentSize() + "]");
        }

        // Load the content of the attachment
        String content;
        try (InputStream stream = attachment.getContentInputStream(xcontext)) {
            if (attachment.getCharset() != null) {
                // Parse the content with the stored encoding
                content = IOUtils.toString(stream, attachment.getCharset());
            } else {
                // Use UTF-8 by default
                content = IOUtils.toString(stream, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to read content of attachment [" + entityReference + "]", e);
        }

        return load(reference, content);
    }

    protected abstract T load(MacroContentSourceReference reference, String content) throws MacroExecutionException;
}
