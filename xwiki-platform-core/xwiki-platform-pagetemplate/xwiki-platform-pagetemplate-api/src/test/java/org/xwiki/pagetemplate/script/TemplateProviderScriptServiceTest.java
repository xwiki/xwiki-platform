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
package org.xwiki.pagetemplate.script;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.pagetemplate.TemplateProviderManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateProviderScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class TemplateProviderScriptServiceTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private TemplateProviderScriptService scriptService;

    @MockComponent
    private TemplateProviderManager templateProviderManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Test
    void getTemplateProvidersKeepsTheOnesTheUserCanView() throws Exception
    {
        TemplateProvider viewable = new TemplateProvider(new DocumentReference("wiki", "Movies", "MovieProvider"));
        TemplateProvider hidden = new TemplateProvider(new DocumentReference("wiki", "Private", "SecretProvider"));
        when(this.templateProviderManager.getTemplateProviders()).thenReturn(List.of(viewable, hidden));
        when(this.authorization.hasAccess(Right.VIEW, viewable.getReference())).thenReturn(true);

        assertEquals(List.of(viewable), this.scriptService.getTemplateProviders());
    }

    @Test
    void getTemplateProvidersWhenTheyCannotBeLoaded() throws Exception
    {
        when(this.templateProviderManager.getTemplateProviders())
            .thenThrow(new TemplateProviderException("Failed to find the template providers.", null));

        assertEquals(List.of(), this.scriptService.getTemplateProviders());
        assertEquals("Failed to list the template providers. Root cause is "
            + "[TemplateProviderException: Failed to find the template providers.].", this.logCapture.getMessage(0));
    }
}
