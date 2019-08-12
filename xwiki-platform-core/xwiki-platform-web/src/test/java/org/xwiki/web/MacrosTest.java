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

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.velocity.tools.EscapeTool;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@code macros.vm}.
 *
 * @version $Id$
 */
public class MacrosTest
{
    private static final String BASEDIR = System.getProperty("basedir");

    private VelocityEngine ve;

    private VelocityContext context;

    @Before
    public void setup()
    {
        this.ve = new VelocityEngine();
        Properties props = new Properties();
        String path = String.format("%s/src/main/webapp/templates", BASEDIR);
        props.setProperty("file.resource.loader.path", path);
        props.setProperty("directive.set.null.allowed", "true");
        props.setProperty("velocimacro.permissions.allow.inline.local.scope", "true");
        props.setProperty("velocimacro.library", "macros.vm");
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
}
