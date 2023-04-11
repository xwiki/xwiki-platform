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
package org.xwiki.whatsnew;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.whatsnew.internal.CategoriesConverter;
import org.xwiki.whatsnew.script.NewsScriptService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Page test for the {@code whatsnew.vm} template.
 *
 * @version $Id$
 * @since 15.2RC1
 */
@ComponentList({
    // Needed to convert a string into news categories, when used in whatsnew.vm
    CategoriesConverter.class
})
class WhatsNewPageTest extends PageTest
{
    @Mock
    private NewsScriptService newsScriptService;

    private TemplateManager templateManager;

    @Test
    void executeTemplateWhenNoNewsItems() throws Exception
    {
        when(this.newsScriptService.getConfiguredNewsSource()).thenReturn(new EmptyNewsSource());
        this.oldcore.getMocker().registerComponent(ScriptService.class, "whatsnew", this.newsScriptService);

        this.templateManager = this.componentManager.getInstance(TemplateManager.class);
        Document html = Jsoup.parse(this.templateManager.render("whatsnew.vm"));

        assertTrue(html.text().contains("No news!"));
    }
}
