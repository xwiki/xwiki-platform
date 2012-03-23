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
package org.xwiki.validator;

import org.xml.sax.SAXParseException;
import org.xwiki.validator.framework.XMLErrorHandler;

/**
 * Extends {@link XMLErrorHandler} to filter some errors.
 * 
 * @version $Id$
 */
public class RSSErrorHandler extends XMLErrorHandler
{
    @Override
    public void error(SAXParseException e)
    {
        // FIXME: Can't find any website producing a RSS content with DOCTYPE element. Also our RSS is produced by ROME
        // so i guess it's supposed to be valid...
        if (!e.getMessage().equals("Document root element \"rss\", must match DOCTYPE root \"null\".")
            && !e.getMessage().equals("Document is invalid: no grammar found.")) {
            super.error(e);
        }
    }
}
