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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Liquibase accessor that provide access to changelogs stored in DataMigration.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class MigrationResourceAccessor extends ClassLoaderResourceAccessor
{
    /** Name for which the change log is served. */
    public static final String CHANGELOG_NAME = "liquibase.xml";

    /** The dynamic change log file. */
    private byte[] changeLog;
    
    /**
     * Create a new accessor to support the dynamically created change log.
     * @param changeLog the dynamically created change log
     */
    public MigrationResourceAccessor(String changeLog)
    {
        super();
        try {
            this.changeLog = changeLog.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // UTF-8 encoding is always available.
        }
    }

    @Override
    public InputStream getResourceAsStream(String file) throws IOException
    {
        if (CHANGELOG_NAME.equals(file)) {
            return new ByteArrayInputStream(changeLog);
        } else {
            return super.getResourceAsStream(file);
        }
    }
}
