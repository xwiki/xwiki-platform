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
package com.xpn.xwiki.internal.filter.input;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.internal.input.AbstractBeanEntityEventGenerator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.internal.filter.XWikiAttachmentFilter;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
// TODO: add support for real revision events (instead of the jrcs archive )
public class XWikiAttachmentEventGenerator
    extends AbstractBeanEntityEventGenerator<XWikiAttachment, XWikiAttachmentFilter, DocumentInstanceInputProperties>
{
    /**
     * The role of this component.
     */
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiAttachment.class, DocumentInstanceInputProperties.class);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public void write(XWikiAttachment attachment, Object filter, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        FilterEventParameters attachmentParameters = new FilterEventParameters();

        if (attachment.getAuthor() != null) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, attachment.getAuthor());
        }
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, attachment.getComment());
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION_DATE, attachment.getDate());
        attachmentParameters.put(WikiAttachmentFilter.PARAMETER_REVISION, attachment.getVersion());

        if (StringUtils.isNotEmpty(attachment.getMimeType())) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_MIMETYPE, attachment.getMimeType());
        }

        if (properties.isWithJRCSRevisions()) {
            try {
                // We need to make sure content is loaded
                XWikiAttachmentArchive archive;
                archive = attachment.loadArchive(xcontext);
                if (archive != null) {
                    attachmentParameters.put(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS,
                        archive.getArchiveAsString());
                }
            } catch (XWikiException e) {
                this.logger.error("Attachment [{}] has malformed history", attachment.getReference(), e);
            }
        }

        InputStream content;
        Long size;
        if (properties.isWithWikiAttachmentsContent()) {
            try {
                content = attachment.getContentInputStream(xcontext);
                size = attachment.getLongSize();
            } catch (XWikiException e) {
                this.logger.error("Failed to get content of attachment [{}]", attachment.getReference(), e);

                content = new ByteArrayInputStream(new byte[0]);
                size = 0L;
            }
        } else {
            content = null;
            size = attachment.getLongSize();
        }

        // WikiAttachment

        attachmentFilter.onWikiAttachment(attachment.getFilename(), content, size, attachmentParameters);
    }
}
