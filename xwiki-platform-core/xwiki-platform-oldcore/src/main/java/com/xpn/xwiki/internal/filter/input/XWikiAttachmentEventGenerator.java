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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.input.DefaultByteArrayInputSource;
import org.xwiki.filter.input.InputSource;
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
public class XWikiAttachmentEventGenerator
    extends AbstractBeanEntityEventGenerator<XWikiAttachment, XWikiAttachmentFilter, DocumentInstanceInputProperties>
{
    /**
     * The role of this component.
     */
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        XWikiAttachment.class, DocumentInstanceInputProperties.class);

    private static class AttachmentSource
    {
        private final long size;

        private final InputSource source;

        private final String alias;

        AttachmentSource(long size, InputSource source, String alias)
        {
            this.size = size;
            this.source = source;
            this.alias = alias;
        }
    }

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public void write(XWikiAttachment attachment, Object filter, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        FilterEventParameters attachmentParameters = getAttachmentParameters(attachment);

        if (properties.isWithWikiAttachmentJRCSRevisions()) {
            try {
                // We need to make sure content is loaded
                XWikiAttachmentArchive archive = attachment.loadArchive(xcontext);
                if (archive != null) {
                    attachmentParameters.put(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS,
                        archive.getArchiveAsString(xcontext));
                }
            } catch (XWikiException e) {
                this.logger.error("Attachment [{}] has malformed history", attachment.getReference(), e);
            }
        }

        writeAttachment(attachment, attachmentFilter, properties, attachmentParameters, xcontext);
    }

    private void writeAttachment(XWikiAttachment attachment, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties, FilterEventParameters attachmentParameters, XWikiContext xcontext)
        throws FilterException
    {
        if (properties.isWithWikiAttachmentsRevisions()) {
            beginEndAttachment(attachment, attachmentFilter, properties, attachmentParameters, xcontext);
        } else {
            onAttachment(attachment, attachmentFilter, properties, attachmentParameters, xcontext);
        }
    }

    private void beginEndAttachment(XWikiAttachment attachment, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties, FilterEventParameters attachmentParameters, XWikiContext xcontext)
        throws FilterException
    {
        AttachmentSource source = getAttachmentSource(attachment, properties, xcontext);

        attachmentFilter.beginWikiDocumentAttachment(attachment.getFilename(), source.source, source.size,
            attachmentParameters);

        try {
            writeRevisions(attachment, attachmentFilter, properties, xcontext);
        } catch (XWikiException e) {
            this.logger.error("Failed to write revisions for attachment [{}]", attachment.getReference(), e);
        }

        attachmentFilter.endWikiDocumentAttachment(attachment.getFilename(), source.source, source.size,
            attachmentParameters);
    }

    private AttachmentSource getAttachmentSource(XWikiAttachment attachment, DocumentInstanceInputProperties properties,
        XWikiContext xcontext)
    {
        return getAttachmentSource(attachment, properties, null, xcontext);
    }

    private AttachmentSource getAttachmentSource(XWikiAttachment attachmentRevision,
        DocumentInstanceInputProperties properties, XWikiAttachment rootAttachment, XWikiContext xcontext)
    {
        InputSource source;
        Long size;
        String alias = null;
        if (isWithWikiAttachmentContent(attachmentRevision, properties)) {
            try {
                source = new XWikiAttachmentContentInputSource(attachmentRevision.getAttachmentContent(xcontext));
                size = attachmentRevision.getLongSize();
            } catch (XWikiException e) {
                this.logger.error("Failed to create the source from the content of the attachment [{}]",
                    attachmentRevision.getReference(), e);

                source = new DefaultByteArrayInputSource(new byte[0]);
                size = 0L;
            }

            if (rootAttachment != null) {
                // Check if the content is the same as another revision
                try {
                    alias = getAlias(attachmentRevision, rootAttachment, xcontext);
                } catch (Exception e) {
                    this.logger.error(
                        "Failed to search for an alias of the content of attachment [{}] in revision [{}]",
                        rootAttachment.getReference(), attachmentRevision, e);
                }
            }
        } else {
            source = null;
            size = attachmentRevision.getLongSize();
        }

        return new AttachmentSource(size, source, alias);
    }

    private String getAlias(XWikiAttachment attachmentRevision, XWikiAttachment rootAttachment, XWikiContext xcontext)
        throws XWikiException, IOException
    {
        if (contentEquals(attachmentRevision, rootAttachment, xcontext)) {
            return WikiAttachmentFilter.VALUE_REVISION_CONTENT_ALIAS_CURRENT;
        }

        String revisionVersion = attachmentRevision.getVersion();

        Version[] versions = rootAttachment.getVersions();

        if (ArrayUtils.isNotEmpty(versions)) {
            for (Version version : versions) {
                String versionString = version.toString();

                if (revisionVersion.contentEquals(versionString)) {
                    return null;
                }

                try {
                    XWikiAttachment revision = rootAttachment.getAttachmentRevision(versionString, xcontext);

                    if (contentEquals(revision, attachmentRevision, xcontext)) {
                        return versionString;
                    }
                } catch (XWikiException e) {
                    this.logger.error("Failed to get the revision [{}] for attachment [{}]", versionString,
                        rootAttachment.getReference(), e);
                }

            }
        }

        return null;
    }

    private boolean contentEquals(XWikiAttachment attachment1, XWikiAttachment attachment2, XWikiContext xcontext)
        throws XWikiException, IOException
    {
        // Compare the size first
        if (attachment1.getContentLongSize(xcontext) == attachment2.getContentLongSize(xcontext)) {
            // Then compare the actual content
            try (InputStream stream1 = attachment1.getContentInputStream(xcontext)) {
                try (InputStream stream2 = attachment2.getContentInputStream(xcontext)) {
                    return IOUtils.contentEquals(stream1, stream2);
                }
            }
        }

        return false;
    }

    private boolean isWithWikiAttachmentContent(XWikiAttachment attachmentRevision,
        DocumentInstanceInputProperties properties)
    {
        if (properties.isWithWikiAttachmentsContent()) {
            return properties.getAttachmentsContent().isEmpty()
                || properties.getAttachmentsContent().contains(attachmentRevision.getFilename());
        }

        return false;
    }

    private void onAttachment(XWikiAttachment attachment, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties, FilterEventParameters attachmentParameters, XWikiContext xcontext)
        throws FilterException
    {
        InputStream content;
        Long size;
        if (isWithWikiAttachmentContent(attachment, properties)) {
            try {
                content = attachment.getContentInputStream(xcontext);
                size = attachment.getLongSize();
            } catch (XWikiException e) {
                this.logger.error("Failed to get the content of the attachment [{}]", attachment.getReference(), e);

                content = new ByteArrayInputStream(new byte[0]);
                size = 0L;
            }
        } else {
            content = null;
            size = attachment.getLongSize();
        }

        attachmentFilter.onWikiAttachment(attachment.getFilename(), content, size, attachmentParameters);
    }

    private void writeRevisions(XWikiAttachment attachment, XWikiAttachmentFilter attachmentFilter,
        DocumentInstanceInputProperties properties, XWikiContext xcontext) throws FilterException, XWikiException
    {
        XWikiAttachmentArchive archive = attachment.getAttachmentArchive(xcontext);

        if (archive != null) {
            Version[] versions = archive.getVersions();

            if (ArrayUtils.isNotEmpty(versions)) {
                attachmentFilter.beginWikiAttachmentRevisions(FilterEventParameters.EMPTY);

                for (Version version : versions) {
                    String versionString = version.toString();
                    XWikiAttachment revision = attachment.getAttachmentRevision(versionString, xcontext);

                    FilterEventParameters parameters = getAttachmentParameters(revision);
                    AttachmentSource source = getAttachmentSource(revision, properties, attachment, xcontext);

                    if (source.alias != null) {
                        parameters.put(WikiAttachmentFilter.PARAMETER_REVISION_CONTENT_ALIAS, source.alias);

                    }

                    attachmentFilter.beginWikiAttachmentRevision(versionString, source.source, source.size, parameters);
                    attachmentFilter.endWikiAttachmentRevision(versionString, source.source, source.size, parameters);
                }

                attachmentFilter.endWikiAttachmentRevisions(FilterEventParameters.EMPTY);
            }
        }
    }

    private FilterEventParameters getAttachmentParameters(XWikiAttachment attachment)
    {
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
        if (StringUtils.isNotEmpty(attachment.getCharset())) {
            attachmentParameters.put(WikiAttachmentFilter.PARAMETER_CHARSET, attachment.getCharset());
        }

        return attachmentParameters;
    }
}
