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
package org.xwiki.extension.security;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.extension.security.notifications.NewExtensionSecurityVulnerabilityTargetableEvent;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of the mail notification template for {@link NewExtensionSecurityVulnerabilityTargetableEvent}.
 *
 * @version $Id$
 * @since 15.5
 */
@ComponentList({
    TemplateScriptService.class
})
class NotificationMailPageTest extends PageTest
{
    public static final String TEMPLATE_PATH =
        "notification/email/org.xwiki.extension.security.notifications.NewExtensionSecurityVulnerabilityTargetableEvent.html.vm";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private VelocityManager velocityManager;

    @Test
    void notification() throws Exception
    {
        DefaultEvent event = new DefaultEvent();
        event.setBody("15");
        CompositeEvent group = new CompositeEvent(event);
        this.velocityManager.getVelocityContext().put("event", group);
        String render = this.templateManager.render(TEMPLATE_PATH);
        Document document = Jsoup.parse(render);
        assertEquals("extension.security.notification.applicationName",
            document.select("td:nth-child(1)").text());
        assertEquals("extension.security.notification.content [15]", document.select("td:nth-child(2)").text());
    }
}
