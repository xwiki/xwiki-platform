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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import java.util.List;

import org.xwiki.extension.security.internal.analyzer.osv.model.PackageObject;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * See the <a href="https://ossf.github.io/osv-schema/#affectedpackage-field">Open Source Vulnerability format API
 * documentation</a>.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class AffectObject
{
    private PackageObject packag;

    private List<RangeObject> ranges;

    /**
     * @return the package
     */
    public PackageObject getPackage()
    {
        return this.packag;
    }

    /**
     * @param packag the package
     */
    public void setPackage(PackageObject packag)
    {
        this.packag = packag;
    }

    /**
     * @return the ranges
     */
    public List<RangeObject> getRanges()
    {
        return this.ranges;
    }

    /**
     * @param ranges the ranges
     */
    public void setRanges(List<RangeObject> ranges)
    {
        this.ranges = ranges;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("package", getPackage())
            .append("ranges", getRanges())
            .toString();
    }
}
