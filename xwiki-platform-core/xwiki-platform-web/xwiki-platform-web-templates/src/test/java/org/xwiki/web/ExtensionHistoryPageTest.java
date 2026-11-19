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
package org.xwiki.web;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.extension.job.history.ExtensionJobHistoryRecord;
import org.xwiki.extension.script.ExtensionHistoryScriptService;
import org.xwiki.extension.script.ExtensionHistoryScriptService.ExtensionHistoryFilter;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of template {@code extensionHistory.vm}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateScriptService.class
})
class ExtensionHistoryPageTest extends PageTest
{
    /**
     * More than the number of history records displayed at once (10), so that the selected records can come from the
     * next pages.
     */
    private static final int RECORD_COUNT = 25;

    private XWikiDocument currentDocument;

    @Mock
    private ExtensionManagerScriptService extensionScriptService;

    @Mock
    private ExtensionHistoryScriptService historyScriptService;

    @Mock
    private ExtensionHistoryFilter recordsFilter;

    private final List<ExtensionJobHistoryRecord> records = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception
    {
        // The records of the local history, the most recent first.
        for (int i = 1; i <= RECORD_COUNT; i++) {
            ExtensionJobHistoryRecord historyRecord = mock(ExtensionJobHistoryRecord.class);
            when(historyRecord.getId()).thenReturn(getRecordId(i));
            this.records.add(historyRecord);
        }

        this.componentManager.registerComponent(ScriptService.class, "extension", this.extensionScriptService);
        doReturn(this.historyScriptService).when(this.extensionScriptService).get("history");
        when(this.historyScriptService.getRecords()).thenReturn(this.recordsFilter);
        when(this.historyScriptService.serialize(any()))
            .thenAnswer(invocation -> "serialized-" + invocation.<ExtensionJobHistoryRecord>getArgument(0).getId());
        when(this.recordsFilter.fromThisWiki()).thenReturn(this.recordsFilter);
        when(this.recordsFilter.ofType(any())).thenReturn(this.recordsFilter);
        // Behave like the real history: the records after the offset record, at most the given number of them.
        when(this.recordsFilter.list(any(), anyInt())).thenAnswer(invocation -> {
            String offsetRecordId = invocation.getArgument(0);
            int limit = invocation.getArgument(1);
            int start = 0;
            if (offsetRecordId != null) {
                while (start < this.records.size() && !this.records.get(start).getId().equals(offsetRecordId)) {
                    start++;
                }
                start++;
            }
            return this.records.subList(Math.min(start, this.records.size()),
                Math.min(start + limit, this.records.size()));
        });

        this.currentDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "XWiki", "ExtensionHistory"), this.context);
        this.context.setDoc(this.currentDocument);
        this.context.setAction("view");

        this.request.put("action", "export");
    }

    @Test
    void exportSelectedRecordFromTheNextPages() throws Exception
    {
        // The 11th record is not on the first page of records (10 records are displayed at once).
        select(11);

        assertEquals("serialized-record-11", export());
    }

    @Test
    void exportSelectedRecordsFromAllThePages() throws Exception
    {
        // The most recent record, the last record of the first page, the first record of the second page and the oldest
        // record. The selected records are exported starting with the oldest one.
        select(1, 10, 11, RECORD_COUNT);

        assertEquals("serialized-record-25 serialized-record-11 serialized-record-10 serialized-record-1", export());
    }

    @Test
    void exportAllRecords() throws Exception
    {
        select(IntStream.rangeClosed(1, RECORD_COUNT).toArray());

        // All the records are exported (not only the first 10), starting with the oldest one.
        String expected = IntStream.iterate(RECORD_COUNT, i -> i >= 1, i -> i - 1)
            .mapToObj(i -> "serialized-" + getRecordId(i)).collect(Collectors.joining(" "));
        assertEquals(expected, export());
    }

    @Test
    void exportSelectedRecordIgnoresTheOffsetRecord() throws Exception
    {
        // The offset is used to load more records, not to look for the selected records.
        this.request.put("offsetRecordId", getRecordId(5));
        select(2);

        assertEquals("serialized-record-2", export());
    }

    private static String getRecordId(int number)
    {
        return "record-" + number;
    }

    private void select(int... recordNumbers)
    {
        // Putting a request parameter again adds a value to it.
        for (int recordNumber : recordNumbers) {
            this.request.put("extensionHistoryRecord", getRecordId(recordNumber));
        }
    }

    private String export() throws Exception
    {
        this.currentDocument.setContent("""
            {{velocity}}
            #template('extensionHistory.vm')
            #handleExtensionHistoryRequest
            {{/velocity}}""");
        this.currentDocument.setSyntax(Syntax.XWIKI_2_1);
        return Jsoup.parse(this.currentDocument.getRenderedContent(this.context)).text();
    }
}
