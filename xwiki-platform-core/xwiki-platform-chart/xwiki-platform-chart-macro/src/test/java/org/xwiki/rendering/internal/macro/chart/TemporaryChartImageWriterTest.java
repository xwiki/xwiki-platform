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
package org.xwiki.rendering.internal.macro.chart;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.macro.chart.ChartMacroParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemporaryChartImageWriter}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@ComponentTest
class TemporaryChartImageWriterTest
{
    @InjectMockComponents
    private TemporaryChartImageWriter writer;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private Environment environment;

    @MockComponent
    private DocumentAccessBridge dab;

    @Test
    void getStorageLocation() throws Exception
    {
        WikiReference currentWikiReference = new WikiReference("wiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        when(this.environment.getTemporaryDirectory()).thenReturn(new File("/tmpdir"));

        File location = this.writer.getStorageLocation(new ImageId(new ChartMacroParameters()));
        assertTrue(location.toString().matches("/tmpdir/temp/chart/wiki/space/page/.*\\.png"),
            "Got: " + location);
    }

    @Test
    void getURL() throws Exception
    {
        WikiReference currentWikiReference = new WikiReference("wiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        when(this.dab.getDocumentURL(new DocumentReference("wiki", "space", "page"), "temp", null, null)).thenReturn(
            "temp/Space/Page");

        String location = this.writer.getURL(new ImageId(new ChartMacroParameters()));
        assertTrue(location.matches("temp/Space/Page/chart/.*\\.png"), "Got: " + location);
    }
}
