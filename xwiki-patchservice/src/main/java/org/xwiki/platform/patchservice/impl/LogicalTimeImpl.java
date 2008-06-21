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
 *
 */
package org.xwiki.platform.patchservice.impl;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.LogicalTime;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link LogicalTime}.
 * 
 * @see org.xwiki.platform.patchservice.api.LogicalTime
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class LogicalTimeImpl implements LogicalTime, Serializable
{
    /** The name of the XML element corresponding to logical time objects. */
    public static final String NODE_NAME = "logicalTime";

    public LogicalTimeImpl()
    {
    }

    public LogicalTimeImpl(Element e) throws XWikiException
    {
        fromXml(e);
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(NODE_NAME);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(LogicalTime other)
    {
        if (!(other instanceof LogicalTimeImpl)) {
            return -1;
        }
        return 0;
    }
}
