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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.tools.EscapeTool;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code macros.vm}.
 *
 * @version $Id$
 */
class MacrosTest extends PageTest
{
    private VelocityManager velocityManager;

    @BeforeEach
    void setup() throws Exception
    {
        this.velocityManager = oldcore.getMocker().getInstance(VelocityManager.class);
        this.velocityManager.getVelocityContext().put("escapetool", new EscapeTool());
    }

    @Test
    void addLivetableLocationFilter() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox')\n"
            + "$whereQL $whereParams"));
        assertEquals("AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!' [%sandbox%]", writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenFilteringWebHome() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true)\n"
            + "$whereQL $whereParams"));
        assertEquals("AND ((doc.name = 'WebHome' AND LOWER(doc.space) LIKE LOWER(?) ESCAPE '!') OR "
            + "(doc.name <> 'WebHome' AND LOWER(doc.fullName) LIKE LOWER(?) ESCAPE '!')) [%sandbox%, %sandbox%]",
            writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenCustomWhere() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', false, 'SOMETHING')\n"
            + "$whereQL $whereParams"));
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    void addLivetableLocationFilterWhenFilteringWebHomeAndCustomWhere() throws Exception
    {
        StringWriter writer = new StringWriter();
        this.velocityManager.evaluate(writer, "logtag", new StringReader("\n"
            + "#set ($whereQL = '')\n"
            + "#set ($whereParams = [])\n"
            + "#addLivetableLocationFilter($whereQL, $whereParams, 'sandbox', true, 'SOMETHING')\n"
            + "$whereQL $whereParams"));
        assertEquals("SOMETHING []", writer.toString().trim());
    }

    @Test
    void livetableFilterObfuscatedTotalGreaterThanReturnedRows() throws Exception
    {
        Map<Object, Object> mapParameter = new HashMap<>();
        mapParameter.put("totalrows", 10);
        mapParameter.put("returnedrows", 5);
        List<String> dummyRows = asList("a", "b", "c");
        mapParameter.put("rows", dummyRows);
        this.velocityManager.getVelocityContext().put("map", mapParameter);
        this.velocityManager.evaluate(new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<Object, Object> map = (Map<Object, Object>) this.velocityManager.getVelocityContext().get("map");
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
        this.velocityManager.getVelocityContext().put("map", mapParameter);
        this.velocityManager.evaluate(new StringWriter(), "livetable",
            new StringReader("#livetable_filterObfuscated($map)"));
        Map<String, Object> map = (Map<String, Object>) this.velocityManager.getVelocityContext().get("map");
        assertEquals(1, map.get("totalrows"));
        assertEquals(1, map.get("returnedrows"));
        assertEquals(1, ((List<?>) map.get("rows")).size());
        assertTrue(((List<Map<String, Boolean>>) map.get("rows")).get(0).get("doc_viewable"));
    }
}
