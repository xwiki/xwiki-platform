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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.velocity.tools.EscapeTool;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@code macros.vm}.
 *
 * @version $Id$
 */
public class MacrosTest
{
    private static final String TEMPLATES_PATH = "src/main/webapp/templates";

    private VelocityEngine ve;

    private VelocityContext context;

    @BeforeEach
    public void setup()
    {
        this.ve = new VelocityEngine();
        Properties props = new Properties();
        props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, TEMPLATES_PATH);
        props.setProperty(RuntimeConstants.VM_PERM_INLINE_LOCAL, Boolean.TRUE.toString());
        props.setProperty(RuntimeConstants.VM_LIBRARY, "macros.vm");
        this.ve.init(props);

        this.context = new VelocityContext();
        this.context.put("escapetool", new EscapeTool());
    }

    @Test
    public void addLivetableLocationFilter()
    {
        StringWriter writer = new StringWriter();
        this.ve.evaluate(this.context, writer, "logtag", "\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox')\n"
            + "$whereQL $whereParams");
        assertEquals("AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!' [%sandbox%]", writer.toString().trim());
    }

    @Test
    public void addLivetableLocationFilterWhenFilteringWebHome()
    {
        StringWriter writer = new StringWriter();
        this.ve.evaluate(this.context, writer, "logtag", "\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true)\n"
            + "$whereQL $whereParams");
        assertEquals("AND ((doc.name = 'WebHome' AND LOWER(doc.space) LIKE LOWER(?) ESCAPE '!') OR "
            + "(doc.name <> 'WebHome' AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!')) [%sandbox%, %sandbox%]",
            writer.toString().trim());
    }

    @Test
    public void addLivetableLocationFilterWhenCustomWhere()
    {
        StringWriter writer = new StringWriter();
        this.ve.evaluate(this.context, writer, "logtag", "\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', false, 'SOMETHING')\n"
            + "$whereQL $whereParams");
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    public void addLivetableLocationFilterWhenFilteringWebHomeAndCustomWhere()
    {
        StringWriter writer = new StringWriter();
        this.ve.evaluate(this.context, writer, "logtag", "\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true, 'SOMETHING')\n"
            + "$whereQL $whereParams");
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    void livetableFilterObfuscatedTotalGreaterThanReturnedRows()
    {
        Map<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("totalrows", 10);
        mapParameter.put("returnedrows", 5);
        List<String> dummyRows = asList("a", "b", "c");
        mapParameter.put("rows", dummyRows);
        this.context.put("map", mapParameter);
        this.ve.evaluate(this.context, new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<Object, Object> map = (Map<Object, Object>) this.context.get("map");
        assertEquals(10, map.get("totalrows"));
        assertEquals(5, map.get("returnedrows"));
        assertSame(dummyRows, map.get("rows"));
    }

    @Test
    void livetableFilterObfuscatedTotalLowerThanReturnedRows() throws Exception
    {
        HashMap<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("totalrows", 2);
        mapParameter.put("returnedrows", 2);
        List<Map<Object, Object>> dummyRows = asList(
            singletonMap("doc_viewable", true),
            singletonMap("doc_viewable", false)
        );
        mapParameter.put("rows", dummyRows);
        this.context.put("map", mapParameter);
        this.ve.evaluate(this.context, new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<String, Object> map = (Map<String, Object>) this.context.get("map");
        assertEquals(1, map.get("totalrows"));
        assertEquals(1, map.get("returnedrows"));
        assertEquals(1, ((List<?>) map.get("rows")).size());
        assertTrue(((List<Map<String, Boolean>>) map.get("rows")).get(0).get("doc_viewable"));
    }
}
