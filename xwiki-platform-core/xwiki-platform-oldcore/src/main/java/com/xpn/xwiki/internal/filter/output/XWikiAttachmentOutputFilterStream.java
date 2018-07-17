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

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiAttachmentFilter;
import org.xwiki.filter.event.xwiki.XWikiWikiAttachmentFilter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiAttachmentOutputFilterStream extends AbstractEntityOutputFilterStream<XWikiAttachment>
{
    // Events

    private void setVersion(FilterEventParameters parameters)
    {
        if (parameters.containsKey(WikiAttachmentFilter.PARAMETER_REVISION)) {
            String version = getString(WikiAttachmentFilter.PARAMETER_REVISION, parameters, null);
            if (version != null) {
                if (VALID_VERSION.matcher(version).matches()) {
                    this.entity.setVersion(version);
                } else if (NumberUtils.isDigits(version)) {
                    this.entity.setVersion(version + ".1");
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
        if (this.entity == null) {
            this.entity = new XWikiAttachment();
        }

        this.entity.setFilename(name);
        this.entity.setMimeType(getString(WikiAttachmentFilter.PARAMETER_MIMETYPE, parameters, null));

        if (content != null) {
            try {
                this.entity.setContent(content);
            } catch (IOException e) {
                throw new FilterException("Failed to set attachment content", e);
            }
        }

        // Author

        this.entity
            .setAuthorReference(getUserReference(WikiAttachmentFilter.PARAMETER_REVISION_AUTHOR, parameters, null));

        // Revision

        if (this.properties == null || this.properties.isVersionPreserved()) {
            setVersion(parameters);
            this.entity.setComment(getString(WikiAttachmentFilter.PARAMETER_REVISION_COMMENT, parameters, ""));
            this.entity.setDate(getDate(WikiAttachmentFilter.PARAMETER_REVISION_DATE, parameters, new Date()));

            String revisions = getString(XWikiWikiAttachmentFilter.PARAMETER_JRCSREVISIONS, parameters, null);
            if (revisions != null) {
                try {
                    this.entity.setArchive(revisions);
                } catch (XWikiException e) {
                    throw new FilterException("Failed to set attachment archive", e);
                }
            }

            this.entity.setMetaDataDirty(false);
        }
    }
}
