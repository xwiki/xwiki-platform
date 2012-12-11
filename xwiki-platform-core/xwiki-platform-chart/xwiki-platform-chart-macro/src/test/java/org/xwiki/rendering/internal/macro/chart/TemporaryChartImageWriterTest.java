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

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.macro.chart.ChartMacroParameters;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import junit.framework.Assert;

/**
 * Unit tests for {@link TemporaryChartImageWriter}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@MockingRequirement(TemporaryChartImageWriter.class)
public class TemporaryChartImageWriterTest extends AbstractMockingComponentTestCase<ChartImageWriter>
{
    @Test
    public void getStorageLocation() throws Exception
    {
        final ModelContext modelContext = getComponentManager().getInstance(ModelContext.class);
        final WikiReference currentWikiReference = new WikiReference("wiki");

        final Environment environment = getComponentManager().getInstance(Environment.class);

        getMockery().checking(new Expectations() {{
            oneOf(modelContext).getCurrentEntityReference();
                will(returnValue(currentWikiReference));
            oneOf(environment).getTemporaryDirectory();
                will(returnValue(new File("/tmpdir")));
        }});

        File location = ((TemporaryChartImageWriter) getMockedComponent()).getStorageLocation(
            new ImageId(new ChartMacroParameters()));
        Assert.assertTrue("Got: " + location.toString(),
            location.toString().matches("/tmpdir/temp/chart/wiki/space/page/.*\\.png"));
    }

    @Test
    public void getURL() throws Exception
    {
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(dab).getDocumentURL(new DocumentReference("unused", "space", "page"), "temp", null, null);
                will(returnValue("temp/Space/Page"));
        }});

        String location = getMockedComponent().getURL(new ImageId(new ChartMacroParameters()));
        Assert.assertTrue("Got: " + location, location.toString().matches("temp/Space/Page/chart/.*\\.png"));
    }
}
