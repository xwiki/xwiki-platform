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
package org.xwiki.chart.test.ui;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.chart.test.po.ChartElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the chart macro.
 *
 * @version $Id$
 */
@UITest
class ChartMacroIT
{
    @Test
    @Order(1)
    void chartDataTableProducedByScript(TestUtils setup, TestReference reference) throws Exception
    {
        // The document holding the data table of the chart, where the table is produced by a script.
        DocumentReference dataReference = new DocumentReference("Data", reference.getLastSpaceReference());

        // The document holding the chart macro, saved by an author who has script right.
        setup.loginAsSuperAdmin();
        setup.rest().savePage(reference,
            "{{chart type=\"line\" title=\"Script data\" params=\"document:" + setup.serializeReference(dataReference)
                + "\"/}}", "");

        // Save the charted document with an author who doesn't have script right.
        setup.createUser("chartDataTableProducedByScript", "chartDataTableProducedByScript", null);
        setup.login("chartDataTableProducedByScript", "chartDataTableProducedByScript");
        setup.rest().savePage(dataReference, getDataTableContent("1.0"), "");

        // The script isn't executed, so the charted document has no data table and the chart macro fails, even
        // though the document holding the chart macro has an author with script right.
        setup.loginAsSuperAdmin();
        ViewPage viewPage = setup.gotoPage(reference);
        assertTrue(viewPage.hasRenderingError());
        assertThat(viewPage.getContent(),
            startsWith("Failed to execute the [chart] macro. Cause: [Unable to find a matching data table.]."));
        assertTrue(ChartElement.getChartsInPageContent().isEmpty());

        // Saving the charted document with an author who has script right produces the data table and thus the chart.
        // The content needs to actually change, otherwise the content author isn't updated.
        setup.rest().savePage(dataReference, getDataTableContent("1.5"), "");
        viewPage = setup.gotoPage(reference);
        assertFalse(viewPage.hasRenderingError());
        // The chart image isn't checked for being loaded because its URL uses the "temp" action, which is provided
        // only by the legacy modules, and they aren't part of the minimal distribution used by this test.
        List<ChartElement> charts = ChartElement.getChartsInPageContent();
        assertEquals(1, charts.size());
        assertEquals("Script data", charts.get(0).getTitle());
    }

    private static String getDataTableContent(String firstValue)
    {
        return """
            {{velocity}}
            #set ($value = '%s')
            |$value|3.0|2.5
            |4.0|1.0|2.3
            {{/velocity}}""".formatted(firstValue);
    }
}
