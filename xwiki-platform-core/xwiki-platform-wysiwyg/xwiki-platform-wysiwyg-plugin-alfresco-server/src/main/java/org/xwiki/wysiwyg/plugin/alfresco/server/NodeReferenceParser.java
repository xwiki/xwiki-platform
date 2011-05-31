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
package org.xwiki.wysiwyg.plugin.alfresco.server;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Parses Alfresco repository node references that have the following format:
 * {@code <storeProtocol>://<storeId>/<nodeId>}. E.g.
 * {@code workspace://SpacesStore/7e2c0259-56bf-4fa1-9319-dace0e69ae60}
 * 
 * @version $Id$
 */
public class NodeReferenceParser
{
    /**
     * The regular expression used to split the node reference in its components.
     */
    private static final Pattern SEPARATOR = Pattern.compile("(?:\\:\\/)?\\/");

    /**
     * Parses the given serialized node reference.
     * 
     * @param serializedReference the serialized nodes reference to be parsed
     * @return the corresponding node reference
     */
    public NodeReference parse(String serializedReference)
    {
        if (StringUtils.isEmpty(serializedReference)) {
            return null;
        }
        String[] components = SEPARATOR.split(serializedReference);
        if (components.length != 3) {
            return null;
        }
        return new NodeReference(components[2], new StoreReference(components[0], components[1]));
    }
}
