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

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xwiki.component.manager.ComponentManager;

/**
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class RootHandler extends AbstractHandler
{
    private Map<String, ContentHandler> handlers = new HashMap<String, ContentHandler>();

    public RootHandler()
    {
        super(null, null);
    }

    public RootHandler(ComponentManager componentManager)
    {
        super(componentManager, null);
    }

    public void setHandler(String element, ContentHandler handler)
    {
        this.handlers.put(element, handler);
    }

    @Override
    protected void startHandlerElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        ContentHandler handler = this.handlers.get(qName);

        if (handler != null) {
            setCurrentHandler(handler);
        } else {
            throw new UnknownRootElement(qName);
        }
    }
}
