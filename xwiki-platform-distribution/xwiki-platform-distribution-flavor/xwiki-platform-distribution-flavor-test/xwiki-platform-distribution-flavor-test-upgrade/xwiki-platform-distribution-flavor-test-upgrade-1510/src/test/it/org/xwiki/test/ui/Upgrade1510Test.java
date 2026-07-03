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
package org.xwiki.test.ui;

/**
 * Execute upgrade tests.
 * 
 * @version $Id$
 */
public class Upgrade1510Test extends UpgradeTest
{
    @Override
    protected void setupLogs()
    {
        validateConsole.getLogCaptureConfiguration().registerExpected(
            // We don't ignore anymore property values related to missing xclass fields when reading a XAR file,
            // so when reading the XAR file of AdminSection we find configureGlobally property which is not present
            // in DB since when we imported the file back then we were ignoring those values, hence the warning.
            "Object property [Home » CKEditor » AdminSection] already removed"
        );
    }
}
