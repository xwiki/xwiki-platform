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
package com.xpn.xwiki.internal.filter.output;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.internal.doc.ListAttachmentArchive;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiAttachmentOutputFilterStream extends AbstractEntityOutputFilterStream<XWikiAttachment>
{
    @Inject
    private Logger logger;

    // Events

    private void setVersion(XWikiAttachment attachment, FilterEventParameters parameters)
    {
        if (parameters.containsKey(WikiAttachmentFilter.PARAMETER_REVISION)) {
            String version = getString(WikiAttachmentFilter.PARAMETER_REVISION, parameters, null);
            if (version != null) {
                if (VALID_VERSION.matcher(version).matches()) {
                    attachment.setVersion(version);
                } else if (NumberUtils.isDigits(version)) {
                    attachment.setVersion(version + ".1");
                } else {
                    // TODO: log something, probably a warning
                }
            }
        }
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        fillAttachment(name, content != null ? new DefaultInputStreamInputSource(content) : null, size, parameters);
    }

    @Override
    public void beginWikiDocumentAttachment(String name, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        fillAttachment(name, content, size, parameters);
    }

    @Override
    public void beginWikiAttachmentRevision(String version, InputSource content, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        if (this.entity != null && this.properties.isVersionPreserved()) {
            ListAttachmentArchive archive = getArchive();

            archive.add(createAttachment(this.entity.getFilename(), content, size, parameters));
        }
    }

    private ListAttachmentArchive getArchive()
    {
        ListAttachmentArchive archive = (ListAttachmentArchive) this.entity.getAttachment_archive();

        if (archive == null) {
            archive = new ListAttachmentArchive(this.entity);
            this.entity.setAttachment_archive(archive);
        }

        return archive;
    }

    private void fillAttachment(String name, InputSource source, Long size, FilterEventParameters parameters)
        throws FilterException
    {
        if (this.entity == null) {
            this.entity = new XWikiAttachment();
        }

        fillAttachment(this.entity, name, source, size, parameters);
    }

    private XWikiAttachment createAttachment(String name, InputSource source, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        XWikiAttachment attachment = new XWikiAttachment();

        fillAttachment(attachment, name, source, size, parameters);

        return attachment;
    }

    private void fillAttachment(XWikiAttachment attachment, String name, InputSource source, Long size,
        FilterEventParameters parameters) throws FilterException
    {
        attachment.setFilename(name);
        if (size != null) {
            attachment.setLongSize(size);
        }
        attachment.setMimeType(getString(WikiAttachmentFilter.PARAMETER_MIMETYPE, parameters, null));
        attachment.setCharset(getString(WikiAttachmentFilter.PARAMETER_CHARSET, parameters, null));

        fillAttachmentContent(attachment, source, parameters);

        // Author

        attachment.setAuthorReference(
            getUserDocumentReference(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, parameters, null));

        // Revision

        if (this.properties == null || this.properties.isVersionPreserved()) {
            setVersion(attachment, parameters);
            attachment.setComment(getString(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, parameters, ""));
            attachment.setDate(getDate(WikiAttachmentFilter.PARAMETER_REVISION_DATE, parameters, new Date()));

            String revisions = getString(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS, parameters, null);
            if (revisions != null) {
                try {
                    attachment.setArchive(revisions);
                } catch (XWikiException e) {
                    this.logger.error(
                        "Failed to set the archive for attachment [{}]. This attachment won't have any history.", name,
                        e);
                }
            }

            attachment.setMetaDataDirty(false);
        }
    }

    private void fillAttachmentContent(XWikiAttachment attachment, InputSource source, FilterEventParameters parameters)
        throws FilterException
    {
        if (source != null) {
            if (source instanceof InputStreamInputSource) {
                try (InputStreamInputSource streamSource = (InputStreamInputSource) source) {
                    attachment.setContent(streamSource.getInputStream());
                } catch (IOException e) {
                    throw new FilterException("Failed to set attachment content", e);
                }
            } else {
                throw new FilterException(
                    "Unsupported input stream type [" + source.getClass() + "] for the attachment content");
            }
        } else {
            // Check if the content is the same as another versions or the current content
            fillAttachmentContentAlias(attachment, parameters);
        }
    }

    private void fillAttachmentContentAlias(XWikiAttachment attachment, FilterEventParameters parameters)
        throws FilterException
    {
        String alias = getString(PARAMETER_REVISION_CONTENT_ALIAS, parameters, null);

        if (alias != null) {
            if (alias.equals(VALUE_REVISION_CONTENT_ALIAS_CURRENT)) {
                try (InputStream stream = this.entity.getContentInputStream(null)) {
                    attachment.setContent(stream);
                } catch (Exception e) {
                    throw new FilterException("Failed copy current attachment content", e);
                }
            } else {
                try {
                    InputStream revisionContent = getArchive().getRevisionContentStream(alias, null);

                    if (revisionContent != null) {
                        try (InputStream stream = revisionContent) {
                            attachment.setContent(stream);
                        }
                    }
                } catch (Exception e) {
                    throw new FilterException("Failed to copy content of attachment revision [" + alias + "]", e);
                }
            }
        }
    }
}
