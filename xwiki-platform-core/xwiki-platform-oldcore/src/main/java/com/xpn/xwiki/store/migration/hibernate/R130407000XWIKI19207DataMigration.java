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
 * This migration increase the maximum size of the parent column to the maximum index supported by MySQL: 768.
 *
 * @version $Id$
 * @since 13.4.7
 */
@Component
@Named("R130407000XWIKI19207")
@Singleton
public class R130407000XWIKI19207DataMigration extends R130200001XWIKI18429DataMigration
{
    private static final int MAXSIZE_MIN = 511;

    @Override
    public String getDescription()
    {
        return "Increase the maximum size of the parent column to the maximum index supported by MySQL";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(130406000);
    }
}
