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
package org.xwiki.extension;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * License of an extension.
 * 
 * @version $Id$
 */
public class ExtensionLicense
{
    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getContent()
     */
    private List<String> content;

    /**
     * @param name the name of the license
     * @param content the content of the license
     */
    public ExtensionLicense(String name, List<String> content)
    {
        this.name = name;
        this.content = content;
    }

    /**
     * @return the name of the license
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the content of the license
     */
    public List<String> getContent()
    {
        return this.content;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ExtensionLicense) {
            ExtensionLicense license = (ExtensionLicense) obj;
            // No need to take care of the content, if it's the same name, it's the same license
            return StringUtils.equals(getName(), license.getName());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return getName() != null ? getName().hashCode() : super.hashCode();
    }
}
