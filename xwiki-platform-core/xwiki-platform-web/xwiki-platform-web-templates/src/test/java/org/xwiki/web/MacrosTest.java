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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.tools.EscapeTool;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
