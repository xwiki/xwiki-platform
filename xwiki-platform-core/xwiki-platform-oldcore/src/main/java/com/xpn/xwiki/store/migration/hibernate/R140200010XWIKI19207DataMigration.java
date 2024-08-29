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
 * This migration increase the maximum size of various columns to the maximum index supported by MySQL: 768.
 *
 * @version $Id$
 * @since 13.4.7
 * @since 13.10.2
 * @since 14.0RC1
 */
@Component
@Named("R140200010XWIKI19207")
@Singleton
public class R140200010XWIKI19207DataMigration extends AbstractResizeMigration
{
    @Override
    public String getDescription()
    {
        return "Increase the maximum size of the columns to the maximum index supported by MySQL";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140200010);
    }
}
