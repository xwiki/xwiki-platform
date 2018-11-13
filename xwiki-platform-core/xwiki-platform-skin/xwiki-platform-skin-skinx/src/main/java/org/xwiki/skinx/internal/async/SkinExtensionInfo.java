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
package org.xwiki.skinx.internal.async;

import java.util.Map;

/**
 * Contains a skin extension to use.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class SkinExtensionInfo
{
    private String type;

    private String resource;

    private Map<String, Object> parameters;

    /**
     * @param type the type of skin extension
     * @param resource the resource name
     * @param parameters the parameters
     */
    public SkinExtensionInfo(String type, String resource, Map<String, Object> parameters)
    {
        this.type = type;
        this.resource = resource;
        this.parameters = parameters;
    }

    /**
     * @return the type of skin extension
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @return the resource name
     */
    public String getResource()
    {
        return this.resource;
    }

    /**
     * @return the parameters
     */
    public Map<String, Object> getParameters()
    {
        return this.parameters;
    }
}
