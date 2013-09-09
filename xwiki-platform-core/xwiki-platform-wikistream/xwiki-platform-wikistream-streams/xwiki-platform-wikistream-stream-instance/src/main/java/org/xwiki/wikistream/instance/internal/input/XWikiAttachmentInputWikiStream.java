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
package org.xwiki.wikistream.instance.internal.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiAttachmentFilter;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.instance.internal.XWikiAttachmentFilter;
import org.xwiki.wikistream.instance.internal.XWikiAttachmentProperties;
import org.xwiki.wikistream.xwiki.filter.XWikiWikiAttachmentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
//TODO: add support for real revision events (instead of the jrcs archive )
public class XWikiAttachmentInputWikiStream implements InputWikiStream
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAttachmentInputWikiStream.class);

    private XWikiAttachment attachment;

    private XWikiAttachmentProperties properties;

    private XWikiContext xcontext;

    public XWikiAttachmentInputWikiStream(XWikiAttachment attachment, XWikiContext xcontext,
        XWikiAttachmentProperties properties)
    {
        this.attachment = attachment;
        this.properties = properties;
        this.xcontext = xcontext;
    }

    @Override
    public void read(Object filter) throws WikiStreamException
    {
        XWikiAttachmentFilter attachmentFilter = (XWikiAttachmentFilter) filter;

        // WikiAttachment

        attachmentFilter.beginWikiAttachment(this.attachment.getFilename(), FilterEventParameters.EMPTY);

        // WikiAttachmentRevision

        FilterEventParameters parameters = new FilterEventParameters();

        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, this.attachment.getAuthor());
        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, this.attachment.getComment());
        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, this.attachment.getDate());

        if (this.properties.isWithWikiAttachmentContent()) {
            try {
                parameters.put(WikiAttachmentFilter.PARAMETER_CONTENT, this.attachment.getContentInputStream(this.xcontext));
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to get content for attachment [" + this.attachment.getReference()
                    + "]", e);
            }
        }

        if (this.properties.isWithWikiAttachmentRevisions()) {
            try {
                // We need to make sure content is loaded
                XWikiAttachmentArchive archive;
                archive = this.attachment.loadArchive(this.xcontext);
                if (archive != null) {
                    parameters.put(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS, new String(archive.getArchive()));
                }
            } catch (XWikiException e) {
                LOGGER.error("Attachment [{}] has malformed history", this.attachment.getReference(), e);
            }
        }

        attachmentFilter.beginWikiAttachmentRevision(this.attachment.getVersion(), parameters);

        // /WikiAttachmentRevision

        attachmentFilter.endWikiAttachmentRevision(this.attachment.getVersion(), parameters);

        // /WikiAttachment

        attachmentFilter.endWikiAttachment(this.attachment.getFilename(), FilterEventParameters.EMPTY);
    }
}
