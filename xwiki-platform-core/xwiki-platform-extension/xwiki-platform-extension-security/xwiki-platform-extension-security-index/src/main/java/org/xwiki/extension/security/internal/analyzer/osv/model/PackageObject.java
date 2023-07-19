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
package org.xwiki.extension.security.internal.analyzer.osv.model;

import org.xwiki.text.XWikiToStringBuilder;

/**
 * See the <a href="https://ossf.github.io/osv-schema/">Open Source Vulnerability format API documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class PackageObject
{
    private String ecosystem;

    private String name;

    /**
     * @return the ecosystem field
     */
    public String getEcosystem()
    {
        return this.ecosystem;
    }

    /**
     * @param ecosystem the ecosystem field
     * @return the current object
     */
    public PackageObject setEcosystem(String ecosystem)
    {
        this.ecosystem = ecosystem;
        return this;
    }

    /**
     * @return the package name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the package name
     * @return the current object
     */
    public PackageObject setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("ecosystem", getEcosystem())
            .append("name", getName())
            .toString();
    }
}
