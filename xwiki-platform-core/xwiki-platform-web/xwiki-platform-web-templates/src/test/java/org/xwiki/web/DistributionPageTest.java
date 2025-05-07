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

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code distribution.vm}.
 *
 * @version $Id$
 */
class DistributionPageTest extends PageTest
{
    @MockComponent(classToMock = ExtensionManagerScriptService.class)
    @Named("extension")
    private ScriptService extensionManagerScriptService;

    @Inject
    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        doReturn("test").when(this.oldcore.getSpyXWiki()).getUserPreference("colorTheme", this.context);
    }

    @Test
    void nonExistingExtensionRequest() throws Exception
    {
        String testValue = "<test>";
        this.stubRequest.put("extensionId", testValue);
        this.stubRequest.put("extensionVersionConstraint", testValue);

        ExtensionDependency dependency =
            new DefaultExtensionDependency(testValue, new DefaultVersionConstraint(testValue));

        ExtensionManagerScriptService service = (ExtensionManagerScriptService) this.extensionManagerScriptService;
        when(service.createExtensionDependency(testValue, testValue)).thenReturn(dependency);

        String output = this.templateManager.render("distribution.vm");

        assertThat(output, containsString("""
            <div class="dependency-item extension-item-unknown">
                <span class="extension-name">&#60;test&#62;</span><span
                      class="extension-version">&#60;test&#62;</span>"""));
    }
}
