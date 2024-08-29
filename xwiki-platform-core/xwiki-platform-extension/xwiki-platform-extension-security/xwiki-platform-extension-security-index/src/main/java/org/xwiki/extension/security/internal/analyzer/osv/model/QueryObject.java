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
public class QueryObject
{
    private PackageObject packag;

    private String version;

    /**
     * @return the package field
     */
    public PackageObject getPackage()
    {
        return this.packag;
    }

    /**
     * @param packag the package field
     * @return the current object
     */
    public QueryObject setPackage(PackageObject packag)
    {
        this.packag = packag;
        return this;
    }

    /**
     * @return the version field
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version field
     * @return the current object
     */
    public QueryObject setVersion(String version)
    {
        this.version = version;
        return this;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("package", getPackage())
            .append("version", getVersion())
            .toString();
    }
}
