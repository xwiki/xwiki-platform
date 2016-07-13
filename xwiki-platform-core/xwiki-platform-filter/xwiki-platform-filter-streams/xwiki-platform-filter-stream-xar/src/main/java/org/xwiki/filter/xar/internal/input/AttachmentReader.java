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

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.xar.internal.XARAttachmentModel;
import org.xwiki.filter.xar.internal.XARFilterUtils.EventParameter;
import org.xwiki.filter.FilterException;

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

        public byte[] content;

        public FilterEventParameters parameters = new FilterEventParameters();

        public void send(XARInputFilter proxyFilter) throws FilterException
        {
            proxyFilter.onWikiAttachment(this.name, new ByteArrayInputStream(this.content),
                Long.valueOf(this.content.length), this.parameters);
        }
    }

    @Override
    public WikiAttachment read(XMLStreamReader xmlReader) throws XMLStreamException, FilterException
    {
        WikiAttachment wikiAttachment = new WikiAttachment();

        for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
            String elementName = xmlReader.getLocalName();

            String value = xmlReader.getElementText();

            EventParameter parameter = XARAttachmentModel.ATTACHMENT_PARAMETERS.get(elementName);

            if (parameter != null) {
                Object wsValue = convert(parameter.type, value);
                if (wsValue != null) {
                    wikiAttachment.parameters.put(parameter.name, wsValue);
                }
            } else {
                if (XARAttachmentModel.ELEMENT_NAME.equals(elementName)) {
                    wikiAttachment.name = value;
                } else if (XARAttachmentModel.ELEMENT_CONTENT.equals(elementName)) {
                    wikiAttachment.content = Base64.decodeBase64(value.getBytes());
                }
            }
        }

        return wikiAttachment;
    }
}
