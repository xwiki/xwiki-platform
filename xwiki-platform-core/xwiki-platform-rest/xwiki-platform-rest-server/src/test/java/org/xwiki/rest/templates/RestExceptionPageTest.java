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
package org.xwiki.rest.templates;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the rest Velocity templates.
 *
 * @version $Id$
 * @since 13.8RC1
 * @since 13.7.1
 * @since 13.4.4
 */
@Tag("PageTest")
class RestExceptionPageTest extends PageTest
{
    @Test
    void exception() throws Exception
    {
        IOException cause = new IOException("file not found");

        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        VelocityManager velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);

        velocityManager.getVelocityContext().put("cause", cause);

        String result = templateManager.render("rest/exception.vm");
        Document document = Jsoup.parse(result);

        assertEquals("rest.exception.noMapper [java.io.IOException]",
            document.getElementsByClass("xwikirenderingerror").get(0).text());
        assertEquals(getStackTrace(cause).trim(),
            document.getElementsByClass("xwikirenderingerrordescription").get(0).text());
    }
}
