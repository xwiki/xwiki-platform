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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.macro.chart.ChartMacroParameters;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemporaryChartImageWriter}.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class TemporaryChartImageWriterTest
{
    @Rule
    public MockitoComponentMockingRule<TemporaryChartImageWriter> componentManager =
        new MockitoComponentMockingRule<TemporaryChartImageWriter>(TemporaryChartImageWriter.class);

    @Test
    public void getStorageLocation() throws Exception
    {
        WikiReference currentWikiReference = new WikiReference("wiki");
        ModelContext modelContext = this.componentManager.getInstance(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        Environment environment = this.componentManager.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File("/tmpdir"));

        File location = this.componentManager.getComponentUnderTest().getStorageLocation(
            new ImageId(new ChartMacroParameters()));
        Assert.assertTrue("Got: " + location.toString(),
            location.toString().matches("/tmpdir/temp/chart/wiki/space/page/.*\\.png"));
    }

    @Test
    public void getURL() throws Exception
    {
        WikiReference currentWikiReference = new WikiReference("wiki");
        ModelContext modelContext = this.componentManager.getInstance(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        DocumentAccessBridge dab = this.componentManager.getInstance(DocumentAccessBridge.class);
        when(dab.getDocumentURL(new DocumentReference("wiki", "space", "page"), "temp", null, null)).thenReturn(
            "temp/Space/Page");

        String location = this.componentManager.getComponentUnderTest().getURL(new ImageId(new ChartMacroParameters()));
        Assert.assertTrue("Got: " + location, location.toString().matches("temp/Space/Page/chart/.*\\.png"));
    }
}
