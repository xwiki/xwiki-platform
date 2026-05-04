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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
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
public class AttachmentReader extends AbstractReader implements XARXMLReader<AttachmentReader.WikiAttachmentInputSource>
{
    /**
     * Represents an abstract attachment content, be it an attachment or an attachment revision.
     */
    private abstract static class AbstractContentInputSource extends AbstractInputStreamInputSource
    {
        /**
         * The actual content.
         */
        public DeferredFileOutputStream content;

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

        @Override
        public void close() throws IOException
        {
            // Close the stream
            try  {
                super.close();
            } catch (IOException e) {
                // Ignore exception since we want to delete the temporary file even if we can't close the stream
            }

            // Make sure to delete the temporary file if the content is not in memory
            if (this.content != null) {
                File contentFile = this.content.getFile();
                if (contentFile != null && contentFile.exists()) {
                    Files.delete(contentFile.toPath());
                }
                this.content = null;
            }
        }
    }

    /**
     * Hold information about an attachment revision.
     */
    public static class WikiAttachmentRevisionInputSource extends AbstractContentInputSource
    {
        /**
         * The version of the revision.
         */
        public String version;

        /**
         * The size of the attachment.
         */
        public Long size;

        /**
         * The parameters of the content.
         */
        public FilterEventParameters parameters = new FilterEventParameters();

        /**
         * Send events related to the attachment revision to the proxy filter.
         *
         * @param proxyFilter the proxy filter where to send the events.
         * @throws FilterException in case of problem when sending events.
         */
        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            InputSource inputSource = this.content != null ? this : null;

            try {
                proxyFilter.beginWikiAttachmentRevision(this.version, inputSource, this.size, this.parameters);
                proxyFilter.endWikiAttachmentRevision(this.version, inputSource, this.size, this.parameters);
            } finally {
                IOUtils.closeQuietly(this);
            }
        }
    }

    /**
     * Hold information about an attachment.
     */
    public static class WikiAttachmentInputSource extends AbstractContentInputSource
    {
        /**
         * The name of the attachment.
         */
        public String name;

        /**
         * The size of the attachment.
         */
        public Long size;

        /**
         * The parameters of the content.
         */
        public FilterEventParameters parameters = new FilterEventParameters();

        /**
         * The revisions of the attachment.
         */
        public List<WikiAttachmentRevisionInputSource> revisions = new ArrayList<>();

        /**
         * Send events related to the attachment to the proxy filter.
         *
         * @param proxyFilter the proxy filter where to send the events.
         * @throws FilterException in case of problem when sending events.
         */
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

                            for (WikiAttachmentRevisionInputSource revision : this.revisions) {
                                revision.send(proxyFilter);
                            }

                            proxyFilter.endWikiAttachmentRevisions(FilterEventParameters.EMPTY);
                        }

                        proxyFilter.endWikiDocumentAttachment(this.name, this, this.size, this.parameters);
                    }
                } finally {
                    IOUtils.closeQuietly(this);
                }
            } else {
                proxyFilter.onWikiAttachment(this.name, null, this.size, this.parameters);
            }
        }

        @SuppressWarnings("checkstyle:NoFinalizer")
        @Override
        protected void finalize() throws Throwable
        {
            close();

            super.finalize();
        }
    }

    @Inject
    private Environment environment;

    @Override
    public WikiAttachmentInputSource read(XMLStreamReader xmlReader, XARInputProperties properties)
        throws XMLStreamException, FilterException
    {
        WikiAttachmentInputSource wikiAttachmentSource = new WikiAttachmentInputSource();

        try {
            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String elementName = xmlReader.getLocalName();

                EventParameter parameter = XARAttachmentModel.ATTACHMENT_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, xmlReader.getElementText());
                    if (wsValue != null) {
                        wikiAttachmentSource.parameters.put(parameter.name, wsValue);
                    }
                } else {
                    if (XARAttachmentModel.ELEMENT_NAME.equals(elementName)) {
                        wikiAttachmentSource.name = xmlReader.getElementText();
                    } else if (XARAttachmentModel.ELEMENT_CONTENT_SIZE.equals(elementName)) {
                        wikiAttachmentSource.size = Long.valueOf(xmlReader.getElementText());
                    } else if (XARAttachmentModel.ELEMENT_CONTENT.equals(elementName)) {
                        readContent(xmlReader, wikiAttachmentSource);
                    } else if (XARAttachmentModel.ELEMENT_REVISIONS.equals(elementName)) {
                        // Skip revisions if history is disabled
                        if (properties.isWithHistory()) {
                            readRevisions(xmlReader, wikiAttachmentSource);
                        } else {
                            StAXUtils.skipElement(xmlReader);
                        }
                    } else {
                        unknownElement(xmlReader);
                    }
                }
            }

            return wikiAttachmentSource;
        } catch (Exception e) {
            IOUtils.closeQuietly(wikiAttachmentSource);
            throw e;
        }
    }

    private void readRevisions(XMLStreamReader xmlReader, WikiAttachmentInputSource wikiAttachment)
        throws XMLStreamException, FilterException
    {
        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (XARAttachmentModel.ELEMENT_REVISION.equals(elementName)) {
                wikiAttachment.revisions.add(readRevision(xmlReader));
            }
        }
    }

    private WikiAttachmentRevisionInputSource readRevision(XMLStreamReader xmlReader)
        throws XMLStreamException, FilterException
    {
        WikiAttachmentRevisionInputSource wikiAttachmentRevisionSource = new WikiAttachmentRevisionInputSource();
        try {
            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String elementName = xmlReader.getLocalName();

                EventParameter parameter = XARAttachmentModel.ATTACHMENT_PARAMETERS.get(elementName);

                if (parameter != null) {
                    Object wsValue = convert(parameter.type, xmlReader.getElementText());
                    if (wsValue != null) {
                        wikiAttachmentRevisionSource.parameters.put(parameter.name, wsValue);
                    }
                } else {
                    if (XARAttachmentModel.ELEMENT_REVISION.equals(elementName)) {
                        wikiAttachmentRevisionSource.version = xmlReader.getElementText();
                    } else if (XARAttachmentModel.ELEMENT_CONTENT_SIZE.equals(elementName)) {
                        wikiAttachmentRevisionSource.size = Long.valueOf(xmlReader.getElementText());
                    } else if (XARAttachmentModel.ELEMENT_CONTENT.equals(elementName)) {
                        readContent(xmlReader, wikiAttachmentRevisionSource);
                    } else {
                        unknownElement(xmlReader);
                    }
                }
            }

            return wikiAttachmentRevisionSource;
        } catch (Exception e) {
            IOUtils.closeQuietly(wikiAttachmentRevisionSource);
            throw e;
        }
    }

    private void readContent(XMLStreamReader xmlReader, AbstractContentInputSource content)
        throws XMLStreamException, FilterException
    {
        // We copy the attachment content to use it later. We can't directly send it as a stream because XAR
        // specification does not force any order for the attachment properties and we need to be sure we
        // have everything when sending the event.

        // Allocate a temporary file in case the attachment content is big
        File temporaryFile;
        try {
            temporaryFile =
                File.createTempFile("xar/attachments/attachment", ".bin", this.environment.getTemporaryDirectory());
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
