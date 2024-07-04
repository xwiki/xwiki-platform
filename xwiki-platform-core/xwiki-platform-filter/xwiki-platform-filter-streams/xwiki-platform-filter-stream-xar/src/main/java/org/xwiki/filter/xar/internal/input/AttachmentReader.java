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
package org.xwiki.filter.xar.internal.input;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.AbstractInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.xml.stax.StAXUtils;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class AttachmentReader extends AbstractReader implements XARXMLReader<AttachmentReader.WikiAttachment>
{
    public static class AbstractContent extends AbstractInputStreamInputSource
    {
        public DeferredFileOutputStream content;

        public FilterEventParameters parameters = new FilterEventParameters();

        @Override
        protected InputStream openStream() throws IOException
        {
            InputStream stream;
            if (this.content.isInMemory()) {
                stream = new ByteArrayInputStream(this.content.getData());
            } else {
                stream = new FileInputStream(this.content.getFile());
            }

            return new Base64InputStream(stream);
        }

        public void dispose()
        {
            if (this.content != null) {
                File contentFile = this.content.getFile();
                if (contentFile != null && contentFile.exists()) {
                    contentFile.delete();
                }
            }
        }
    }

    public static class WikiAttachmentRevision extends AbstractContent
    {
        public String version;

        public Long size;

        public FilterEventParameters parameters = new FilterEventParameters();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            InputSource inputSource = this.content != null ? this : null;

            try {
                proxyFilter.beginWikiAttachmentRevision(this.version, inputSource, this.size, this.parameters);
                proxyFilter.endWikiAttachmentRevision(this.version, inputSource, this.size, this.parameters);
            } finally {
                dispose();
            }
        }
    }

    public static class WikiAttachment extends AbstractContent
    {
        public String name;

        public Long size;

        public List<WikiAttachmentRevision> revisions = new ArrayList<>();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            if (this.content != null) {
                try {
                    if (this.revisions.isEmpty()) {
                        try (InputStream stream = openStream()) {
                            proxyFilter.onWikiAttachment(this.name, stream, this.size, this.parameters);
                        } catch (IOException e) {
                            throw new FilterException(e);
                        }
                    } else {
                        proxyFilter.beginWikiDocumentAttachment(this.name, this, this.size, this.parameters);

                        // Send revisions
                        if (!this.revisions.isEmpty()) {
                            proxyFilter.beginWikiAttachmentRevisions(FilterEventParameters.EMPTY);

                            for (WikiAttachmentRevision revision : this.revisions) {
                                revision.send(proxyFilter);
                            }

                            proxyFilter.endWikiAttachmentRevisions(FilterEventParameters.EMPTY);
                        }

                        proxyFilter.endWikiDocumentAttachment(this.name, this, this.size, this.parameters);
                    }
                } finally {
                    if (this.content.isInMemory()) {
                        this.content.getFile().delete();
                    }
                }
            } else {
                proxyFilter.onWikiAttachment(this.name, null, this.size, this.parameters);
            }
        }

        @Override
        protected void finalize() throws Throwable
        {
            // Make sure to get rid of the file (if any)
            if (this.content != null && !this.content.isInMemory() && this.content.getFile() != null
                && this.content.getFile().exists()) {
                this.content.getFile().delete();
            }

            super.finalize();
        }
    }

    @Override
    public WikiAttachment read(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiAttachment wikiAttachment = new WikiAttachment();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            EventParameter parameter = XARAttachmentModel.ATTACHMENT_PARAMETERS.get(elementName);

            if (parameter != null) {
                Object wsValue = convert(parameter.type, xmlReader.getElementText());
                if (wsValue != null) {
                    wikiAttachment.parameters.put(parameter.name, wsValue);
                }
            } else {
                if (XARAttachmentModel.ELEMENT_NAME.equals(elementName)) {
                    wikiAttachment.name = xmlReader.getElementText();
                } else if (XARAttachmentModel.ELEMENT_CONTENT_SIZE.equals(elementName)) {
                    wikiAttachment.size = Long.valueOf(xmlReader.getElementText());
                } else if (XARAttachmentModel.ELEMENT_CONTENT.equals(elementName)) {
                    readContent(xmlReader, wikiAttachment);
                } else if (XARAttachmentModel.ELEMENT_REVISIONS.equals(elementName)) {
                    // Skip revisions if history is disabled
                    if (properties.isWithHistory()) {
                        readRevisions(xmlReader, properties, wikiAttachment);
                    } else {
                        StAXUtils.skipElement(xmlReader);
                    }
                } else {
                    unknownElement(xmlReader);
                }
            }
        }

        return wikiAttachment;
    }

    private void readRevisions(XMLStreamReader xmlReader, XARInputProperties properties, WikiAttachment wikiAttachment)
        throws XMLStreamException, FilterException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (XARAttachmentModel.ELEMENT_REVISION.equals(elementName)) {
                wikiAttachment.revisions.add(readRevision(xmlReader, properties));
            }
        }
    }

    private WikiAttachmentRevision readRevision(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiAttachmentRevision wikiAttachmentRevision = new WikiAttachmentRevision();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            EventParameter parameter = XARAttachmentModel.ATTACHMENT_PARAMETERS.get(elementName);

            if (parameter != null) {
                Object wsValue = convert(parameter.type, xmlReader.getElementText());
                if (wsValue != null) {
                    wikiAttachmentRevision.parameters.put(parameter.name, wsValue);
                }
            } else {
                if (XARAttachmentModel.ELEMENT_REVISION.equals(elementName)) {
                    wikiAttachmentRevision.version = xmlReader.getElementText();
                } else if (XARAttachmentModel.ELEMENT_CONTENT_SIZE.equals(elementName)) {
                    wikiAttachmentRevision.size = Long.valueOf(xmlReader.getElementText());
                } else if (XARAttachmentModel.ELEMENT_CONTENT.equals(elementName)) {
                    readContent(xmlReader, wikiAttachmentRevision);
                } else {
                    unknownElement(xmlReader);
                }
            }
        }

        return wikiAttachmentRevision;
    }

    private void readContent(XMLStreamReader xmlReader, AbstractContent content)
        throws XMLStreamException, FilterException
    {
        // We copy the attachment content to use it later. We can't directly send it as a stream because XAR
        // specification does not force any order for the attachment properties and we need to be sure we
        // have everything when sending the event.

        // Allocate a temporary file in case the attachment content is big
        File temporaryFile;
        try {
            temporaryFile = File.createTempFile("xar/attachments/attachment", ".bin");
        } catch (IOException e) {
            throw new FilterException(e);
        }

        // Create a deferred file based content (if the content is bigger than 10000 bytes it will end up in
        // a file)
        content.content = new DeferredFileOutputStream(100000, temporaryFile);

        // Copy the content to byte array or file depending on its size
        for (xmlReader.next(); xmlReader.isCharacters(); xmlReader.next()) {
            try {
                content.content.write(xmlReader.getText().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new FilterException(e);
            }
        }
    }
}
