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
package org.xwiki.validator.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Used to overwrite the the default entity resolver and get the entities from the resources.
 * 
 * @version $Id$
 */
public class XMLResourcesEntityResolver implements EntityResolver
{
    /**
     * Slash constant.
     */
    private static final String URL_SEPARATOR = "/";
    
    /**
     * Entities directory.
     */
    private static final String ENTITIES_ROOT = "/entities";

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
        int index = systemId.lastIndexOf(URL_SEPARATOR);

        String dtd = ENTITIES_ROOT + (index != -1 ? systemId.substring(index) : URL_SEPARATOR + systemId);
        InputStream stream = XMLResourcesEntityResolver.class.getResourceAsStream(dtd);

        return new InputSource(new InputStreamReader(stream));
    }
}
