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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class AttachmentReader extends AbstractReader implements XARXMLReader<AttachmentReader.WikiAttachment>
{
    public static class WikiAttachment
    {
        public String name;

        public Long size;

        public DeferredFileOutputStream content;

        public FilterEventParameters parameters = new FilterEventParameters();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            if (this.content != null) {
                try (InputStream is = new Base64InputStream(openStream())) {
                    proxyFilter.onWikiAttachment(this.name, is, this.size, this.parameters);
                } catch (IOException e) {
                    throw new FilterException(e);
                } finally {
                    if (this.content.isInMemory()) {
                        this.content.getFile().delete();
                    }
                }
            } else {
                proxyFilter.onWikiAttachment(this.name, null, this.size, this.parameters);
            }
        }

        private InputStream openStream() throws FileNotFoundException
        {
            if (this.content.isInMemory()) {
                return new ByteArrayInputStream(this.content.getData());
            } else {
                return new FileInputStream(this.content.getFile());
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
                    wikiAttachment.content = new DeferredFileOutputStream(100000, temporaryFile);

                    // Copy the content to byte array or file depending on its size
                    for (xmlReader.next(); xmlReader.isCharacters(); xmlReader.next()) {
                        try {
                            wikiAttachment.content.write(xmlReader.getText().getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            throw new FilterException(e);
                        }
                    }
                } else {
                    unknownElement(xmlReader);
                }
            }
        }

        return wikiAttachment;
    }
}
