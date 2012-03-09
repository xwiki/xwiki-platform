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

import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.packager.XarEntry;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

/**
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class XarPageLimitedHandler extends AbstractHandler
{
    private XarEntry xarEntry = new XarEntry();

    private EntityReference pageReference;

    public XarPageLimitedHandler(ComponentManager componentManager)
    {
        super(componentManager);

        setCurrentBean(this);

        this.pageReference = new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE));

        addsupportedElements("name");
        addsupportedElements("web");
        addsupportedElements("language");
        addsupportedElements("defaultLanguage");
    }

    public XarEntry getXarEntry()
    {
        return this.xarEntry;
    }

    @Override
    public void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equals("language")) {
            if (this.value.length() > 0) {
                this.xarEntry.setLanguage(this.value.toString());
            }
        } else if (qName.equals("defaultLanguage")) {
            if (this.xarEntry.getLanguage() == null) {
                this.xarEntry.setLanguage(this.value.toString());
            }
        } else if (qName.equals("name")) {
            this.pageReference = new EntityReference(this.value.toString(), EntityType.DOCUMENT,
               this.pageReference.getParent());
            this.xarEntry.setDocumentReference(this.pageReference);
        } else if (qName.equals("web")) {
            this.pageReference = this.pageReference.replaceParent(this.pageReference.getParent(),
               new EntityReference(this.value.toString(), EntityType.SPACE));
            this.xarEntry.setDocumentReference(this.pageReference);
        } else {
            super.endElementInternal(uri, localName, qName);
        }
    }
}
