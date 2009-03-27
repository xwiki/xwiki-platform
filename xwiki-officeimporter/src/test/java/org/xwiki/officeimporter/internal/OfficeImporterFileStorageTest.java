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
package org.xwiki.officeimporter.internal;

import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Test class for {@link OfficeImporterFileStorage}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class OfficeImporterFileStorageTest extends AbstractXWikiComponentTestCase
{
    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception
    {
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        super.setUp();
    }

    /**
     * Test filtering of invalid file name characters.
     */
    public void testInvalidFileNameCharacterFiltering() throws RuntimeException
    {
        OfficeImporterFileStorage storage = new OfficeImporterFileStorage("Temp/\\:*?\"<>|Dir");
        assertEquals("Temp---------Dir", storage.getTempDir().getName());
        storage.cleanUp();
    }
}
