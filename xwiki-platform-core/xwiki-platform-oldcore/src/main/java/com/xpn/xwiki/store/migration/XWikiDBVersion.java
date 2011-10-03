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
package com.xpn.xwiki.store.migration;

/**
 * This entity is store xwiki's data version in database. Used for migrations.
 * Data version is svn revision number from which data need migration.
 * Immutable.
 * @version $Id$
 */
public class XWikiDBVersion implements Comparable<XWikiDBVersion>
{
    /** svn revision number. */
    private int version;

    /** Default constructor. It is need for Hibernate. */
    public XWikiDBVersion()
    {
    }

    /** @param version - data version */
    public XWikiDBVersion(int version)
    {
        this.version = version;
    }

    /** @return data version */
    public int getVersion()
    {
        return version;
    }

    /** @param version - data version */
    protected void setVersion(int version)
    {
        this.version = version;
    }

    @Override
    public int compareTo(XWikiDBVersion o)
    {
        if (o == null) {
            return -1;
        }
        return Integer.valueOf(getVersion()).compareTo(o.getVersion());
    }

    @Override
    public String toString()
    {
        return String.valueOf(this.version);
    }

    /** @return next version */
    public XWikiDBVersion increment()
    {
        return new XWikiDBVersion(getVersion() + 1);
    }
}
