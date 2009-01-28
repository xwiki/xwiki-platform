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
package org.xwiki.rest.representers;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResourceRepresenter;

/**
 * @version $Id$
 */
public class XmlTextXStreamRepresenter implements XWikiResourceRepresenter
{

    public MediaType getMediaType()
    {
        return MediaType.TEXT_XML;
    }

    public Representation represent(Context context, Request request, Response response, Object... objects)
    {
        return new StringRepresentation(Utils.toXml(objects[0]), getMediaType());
    }

}
