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
package org.xwiki.extension.xar.internal.handler.packager.xml;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class AttachmentHandler extends AbstractHandler
{
    public AttachmentHandler(ComponentManager componentManager)
    {
        super(componentManager, new XWikiAttachment());
    }

    public XWikiAttachment getAttachment()
    {
        return (XWikiAttachment) getCurrentBean();
    }

    @Override
    protected void startElementInternal(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        if (qName.equals("content")) {

        } else if (qName.equals("versions")) {
            this.value = null;
        } else {
            super.startElementInternal(uri, localName, qName, attributes);
        }
    }

    @Override
    protected void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equals("content")) {
            try {
                Base64InputStream b64is = new Base64InputStream(IOUtils.toInputStream(this.value));
                getAttachment().setContent(b64is);
            } catch (IOException e) {
                // TODO: log error
            }
        } else {
            super.endElementInternal(uri, localName, qName);
        }
    }
}
