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
package org.xwiki.livedata.internal.macro;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.embed.EmbeddableComponentManagerFactory;
import org.xwiki.component.internal.multi.DefaultComponentManagerManager;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.internal.DefaultLiveDataConfigurationResolver;
import org.xwiki.livedata.internal.DefaultLiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererConfiguration;
import org.xwiki.livedata.internal.StringLiveDataConfigurationResolver;
import org.xwiki.livedata.internal.script.LiveDataConfigHelper;
import org.xwiki.livedata.script.LiveDataScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.IconSetup;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * @version $Id$
 * @since 15.6RC1
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    TemplateMacro.class,
    LiveDataScriptService.class,
    DefaultLiveDataSourceManager.class,
    DefaultComponentManagerManager.class,
    EmbeddableComponentManagerFactory.class,
    LiveDataConfigHelper.class,
    StringLiveDataConfigurationResolver.class,
    DefaultLiveDataConfigurationResolver.class,
    LiveDataRenderer.class,
    LiveDataRendererConfiguration.class,
    DefaultExtendedRenderingConfiguration.class
})
//@DefaultIconManagerComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
class LiveDataVelocityMacroPageTest extends PageTest
{
    @MockComponent
    private IconManager iconManager;

    @MockComponent
    @Named("jsfx")
    SkinExtension jsfx;

    @Test
    void name() throws Exception
    {
        IconSetup.setUp(this, "/icons/default.iconset");
        when(this.xwiki.getPluginApi(any(), any())).thenReturn(mock(SkinExtensionPluginApi.class));

        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        String content = "{{template name=\"liveData/macro.vm\" output=\"false\"/}}\n"
            + "{{velocity}}\n"
            + "#liveData({'id': 'test'} {} 'xwiki/2.1')\n"
            + "{{/velocity}}";
        System.out.println(content);
        document.setContent(content);

        String renderedContent = document.getRenderedContent(this.context);
        System.out.println(renderedContent);
    }
}
