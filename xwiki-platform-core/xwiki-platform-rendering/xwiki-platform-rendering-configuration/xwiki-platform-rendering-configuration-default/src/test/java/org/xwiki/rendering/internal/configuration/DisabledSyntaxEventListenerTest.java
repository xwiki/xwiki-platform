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
package org.xwiki.rendering.internal.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DisabledSyntaxEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class DisabledSyntaxEventListenerTest
{
    private static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference("Rendering", "RenderingConfigClass");
    private static final LocalDocumentReference DOC_REFERENCE =
        new LocalDocumentReference("Rendering", "RenderingConfig");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @InjectMockComponents
    private DisabledSyntaxEventListener listener;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext xContext;

    @Mock
    private XWikiDocument document;

    @MockComponent
    private ExtendedRenderingConfiguration configuration;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfiguration;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @BeforeComponent
    void beforeComoponent() throws Exception
    {
        Provider<ComponentManager> contextComponentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.xwiki.getDocument(DOC_REFERENCE, this.xContext)).thenReturn(this.document);
    }

    @Test
    void onEventWhenDBErrorHappens() throws Exception
    {
        doThrow(new XWikiException("error", new Throwable())).when(this.xwiki).getDocument(any(EntityReference.class),
            any());

        this.listener.onEvent(new ApplicationReadyEvent(), this.xwiki, this.xContext);

        // Assert the log
        assertEquals("Failed to migrate the rendering configuration by adding a [Rendering.RenderingConfigClass] "
            + "xobject to [Rendering.RenderingConfig]", logCapture.getMessage(0));
    }

    @Test
    void onEventWhenNoConfigXObjectAndExistingXWikiCfgProperty() throws Exception
    {
        when(this.document.getXObject(CLASS_REFERENCE)).thenReturn(null);
        BaseObject xobject = mock(BaseObject.class);
        when(this.document.newXObject(CLASS_REFERENCE, this.xContext)).thenReturn(xobject);
        when(this.xwikiCfgConfiguration.getProperty("xwiki.rendering.syntaxes", List.class))
            .thenReturn(Arrays.asList("default1/1.0", "default2/1.0"));

        // Register some Syntaxes for the test

        Syntax default1Syntax = new Syntax(new SyntaxType("default1", "Default 1"), "1.0");
        Parser default1Parser = this.componentManager.registerMockComponent(Parser.class, default1Syntax.toIdString());
        when(default1Parser.getSyntax()).thenReturn(default1Syntax);

        Syntax default2Syntax = new Syntax(new SyntaxType("default2", "Default 2"), "1.0");
        Parser default2Parser = this.componentManager.registerMockComponent(Parser.class, default2Syntax.toIdString());
        when(default2Parser.getSyntax()).thenReturn(default2Syntax);

        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");
        Parser syntax1Parser = this.componentManager.registerMockComponent(Parser.class, syntax1.toIdString());
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);

        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        Parser syntax2Parser = this.componentManager.registerMockComponent(Parser.class, syntax2.toIdString());
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        this.listener.onEvent(new ApplicationReadyEvent(), this.xwiki, this.xContext);

        // The tests are here below.

        // Configured syntaxes default1 and default2 are not disabled. But the other 2 syntaxes are.
        verify(xobject).set("disabledSyntaxes", List.of("syntax1/1.0", "syntax2/1.0"), this.xContext);

        // Ensure we save the doc.
        verify(this.xwiki).saveDocument(this.document, "Migrated Rendering configuration found in xwiki.cfg "
            + "(xwiki.rendering.syntaxes)", true, this.xContext);
    }

    @Test
    void onEventWhenNoConfigXObjectAndNoXWikiCfgProperty() throws Exception
    {
        when(this.document.getXObject(CLASS_REFERENCE)).thenReturn(null);
        BaseObject xobject = mock(BaseObject.class);
        when(this.document.newXObject(CLASS_REFERENCE, this.xContext)).thenReturn(xobject);
        when(this.xwikiCfgConfiguration.getProperty("xwiki.rendering.syntaxes", List.class))
            .thenReturn(Collections.emptyList());

        // Register some Syntaxes for the test

        Syntax default1Syntax = new Syntax(new SyntaxType("default1", "Default 1"), "1.0");
        Parser default1Parser = this.componentManager.registerMockComponent(Parser.class, default1Syntax.toIdString());
        when(default1Parser.getSyntax()).thenReturn(default1Syntax);
        when(this.configuration.getDefaultContentSyntax()).thenReturn(default1Syntax);

        Syntax syntax1 = new Syntax(new SyntaxType("syntax1", "Syntax 1"), "1.0");
        Parser syntax1Parser = this.componentManager.registerMockComponent(Parser.class, syntax1.toIdString());
        when(syntax1Parser.getSyntax()).thenReturn(syntax1);

        Syntax syntax2 = new Syntax(new SyntaxType("syntax2", "Syntax 2"), "1.0");
        Parser syntax2Parser = this.componentManager.registerMockComponent(Parser.class, syntax2.toIdString());
        when(syntax2Parser.getSyntax()).thenReturn(syntax2);

        this.listener.onEvent(new ApplicationReadyEvent(), this.xwiki, this.xContext);

        // The tests are here below.

        // No configured syntaxes. Thus, all syntaxes except the default one are disabled.
        verify(xobject).set("disabledSyntaxes", List.of("syntax1/1.0", "syntax2/1.0"), this.xContext);

        // Ensure we save the doc.
        verify(this.xwiki).saveDocument(this.document, "Migrated Rendering configuration found in xwiki.cfg "
            + "(xwiki.rendering.syntaxes)", true, this.xContext);
    }

    @Test
    void onEventWhenConfigXObjectExists() throws Exception
    {
        BaseObject xobject = mock(BaseObject.class);
        when(this.document.getXObject(CLASS_REFERENCE)).thenReturn(xobject);

        this.listener.onEvent(new ApplicationReadyEvent(), this.xwiki, this.xContext);

        // The tests are here below.

        // No set in the xobject is done
        verify(xobject, never()).set(any(), any(), any());

        // No document save is done
        verify(this.xwiki).getDocument(any(EntityReference.class), any());
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    void onEventWhenDocumentDoesntExist() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);

        this.listener.onEvent(new ApplicationReadyEvent(), this.xwiki, this.xContext);

        // The tests are here below.

        // No document save is done
        verify(this.xwiki).getDocument(any(EntityReference.class), any());
        verifyNoMoreInteractions(this.xwiki);
    }
}
