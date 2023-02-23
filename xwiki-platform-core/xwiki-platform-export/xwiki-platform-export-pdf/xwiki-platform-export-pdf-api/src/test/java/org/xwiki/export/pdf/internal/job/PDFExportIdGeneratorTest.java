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
package org.xwiki.export.pdf.internal.job;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PDFExportIdGenerator}.
 * 
 * @version $Id$
 */
class PDFExportIdGeneratorTest
{
    private PDFExportIdGenerator idGenerator = new PDFExportIdGenerator();

    @Test
    void removeAndReset()
    {
        Map<String, String> expectedIdMap = new HashMap<>();

        this.idGenerator.generateUniqueId("Alice");
        this.idGenerator.generateUniqueId("Bob");

        expectedIdMap.put("IAlice", "IAlice");
        expectedIdMap.put("IBob", "IBob");
        assertEquals(expectedIdMap, this.idGenerator.resetLocalIds());

        this.idGenerator.generateUniqueId("Alice");
        this.idGenerator.generateUniqueId("Bob");
        this.idGenerator.generateUniqueId("Carol");
        this.idGenerator.remove("IBob-1");

        expectedIdMap.clear();
        expectedIdMap.put("IAlice", "IAlice-1");
        expectedIdMap.put("ICarol", "ICarol");
        assertEquals(expectedIdMap, this.idGenerator.resetLocalIds());

        this.idGenerator.remove("IBob");
        this.idGenerator.generateUniqueId("Alice");
        this.idGenerator.generateUniqueId("Bob");
        this.idGenerator.generateUniqueId("Carol");

        expectedIdMap.clear();
        expectedIdMap.put("IAlice", "IAlice-2");
        expectedIdMap.put("IBob", "IBob");
        expectedIdMap.put("ICarol", "ICarol-1");
        assertEquals(expectedIdMap, this.idGenerator.resetLocalIds());

        this.idGenerator.reset();
        this.idGenerator.generateUniqueId("Alice");
        this.idGenerator.generateUniqueId("Bob");
        this.idGenerator.generateUniqueId("Carol");

        expectedIdMap.clear();
        expectedIdMap.put("IAlice", "IAlice");
        expectedIdMap.put("IBob", "IBob");
        expectedIdMap.put("ICarol", "ICarol");
        assertEquals(expectedIdMap, this.idGenerator.resetLocalIds());
    }
}
