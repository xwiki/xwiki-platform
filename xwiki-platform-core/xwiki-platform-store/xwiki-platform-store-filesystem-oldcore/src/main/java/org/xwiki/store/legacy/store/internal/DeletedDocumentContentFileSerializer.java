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
package org.xwiki.store.legacy.store.internal;

import java.io.File;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.output.DefaultFileOutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.store.FileSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;

/**
 * Serialize a {@link XWikiDocument} in a {@link File}.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
@Component(roles = DeletedDocumentContentFileSerializer.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DeletedDocumentContentFileSerializer implements FileSerializer
{
    @Inject
    private XWikiDocumentFilterUtils serializer;

    private XWikiDocument document;

    private String encoding;

    /**
     * @param document the document to serialize
     * @param encoding the encoding in which to serialize the document
     */
    public void init(XWikiDocument document, String encoding)
    {
        this.document = document;
        this.encoding = encoding;
    }

    @Override
    public void serialize(File file) throws Exception
    {
        // Input
        DocumentInstanceInputProperties documentProperties = new DocumentInstanceInputProperties();
        // Disable streamed page revisions and enable JRCS based one because it matches better what's actually stored by
        // default
        documentProperties.setWithRevisions(false);
        documentProperties.setWithJRCSRevisions(true);
        // Disable JRCS based attachment history and enabled streamed one because the JRCS one takes too much memory
        documentProperties.setWithWikiAttachmentJRCSRevisions(false);
        documentProperties.setWithWikiAttachmentsRevisions(true);

        // Output
        XAROutputProperties xarProperties = new XAROutputProperties();
        xarProperties.setPreserveVersion(true);
        xarProperties.setEncoding(this.encoding);
        xarProperties.setFormat(true);

        try {
            this.serializer.exportEntity(this.document, new DefaultFileOutputTarget(file), xarProperties,
                documentProperties);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }
    }
}
