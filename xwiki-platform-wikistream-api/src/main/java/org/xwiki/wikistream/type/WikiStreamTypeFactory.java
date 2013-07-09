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
package org.xwiki.wikistream.type;

import java.util.List;

/**
 * WikiStream type factory component exposes the information about supported wikis and dataformats
 * 
 * @version $Id$
 */
public interface WikiStreamTypeFactory
{
    /**
     * Create a specific WikiStream type with given Id as String. (eg : "mediawiki/xml", "xwiki/xar", "wordpress/xmlrpc"
     * .. )
     * 
     * @param wikiStreamType Type id as String.
     * @return WikiStreamType
     */
    WikiStreamType createTypeFromIdString(String wikiStreamType) throws WikiStreamTypeException;

    /**
     * @return list of available/supported wiki stream types and dataformats
     */
    List<WikiStreamType> getAvailableTypes() throws WikiStreamTypeException;
}
