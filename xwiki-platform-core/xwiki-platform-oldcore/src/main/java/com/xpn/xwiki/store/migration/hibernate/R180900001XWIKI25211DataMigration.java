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
package com.xpn.xwiki.store.migration.hibernate;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Increase again the maximum size of the columns to the maximum index supported by MySQL: 768. On databases storing
 * the unquoted column names in upper case (e.g. HSQLDB, Oracle), {@link R140200010XWIKI19207DataMigration} could not
 * find the columns declared in lower case in the Hibernate mapping (e.g. the legacy activity stream ones) and thus
 * kept them at their previous size.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Named("R180900001XWIKI25211")
@Singleton
public class R180900001XWIKI25211DataMigration extends AbstractResizeMigration
{
    @Override
    public String getDescription()
    {
        return "Increase the maximum size of the columns which were not found by the previous resize migration";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180900001);
    }
}
