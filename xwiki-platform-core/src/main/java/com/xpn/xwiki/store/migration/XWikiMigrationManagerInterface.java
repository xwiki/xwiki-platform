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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Interface for all migration managers.
 * @version $Id$ 
 */
public interface XWikiMigrationManagerInterface
{
    /**
     * @return data version
     * @param context - used everywhere
     * @xwikicfg xwiki.store.migration.version - override data version
     * @throws XWikiException if any error
     */
    XWikiDBVersion getDBVersion(XWikiContext context) throws XWikiException;
    
    /**
     * @param context - used everywhere
     * @throws XWikiException if any error
     * @xwikicfg xwiki.store.migration.forced  - force run selected migrations and ignore all others
     * @xwikicfg xwiki.store.migration.ignored - ignore selected migrations
     * @throws XWikiException if any error
     */
    void startMigrations(XWikiContext context) throws XWikiException;
}
