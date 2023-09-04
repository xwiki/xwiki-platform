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
package org.xwiki.image.style.internal.configuration;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.internal.configuration.source.ImageStyleConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultImageStyleConfiguration}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class DefaultImageStyleConfigurationTest
{
    private static final String DOCUMENT_REFERENCE_STR = "xwiki:Space.Page";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    @InjectMockComponents
    private DefaultImageStyleConfiguration configuration;

    @MockComponent
    @Named(ImageStyleConfigurationSource.HINT)
    private ConfigurationSource configurationSource;

    @MockComponent
    private ExecutionContextManager contextManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument doc;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.context)).thenReturn(this.doc);
        when(this.documentReferenceResolver.resolve(DOCUMENT_REFERENCE_STR, this.context))
            .thenReturn(DOCUMENT_REFERENCE);
    }

    @Test
    void getDefaultStyle() throws Exception
    {
        this.configuration.getDefaultStyle("wiki", DOCUMENT_REFERENCE_STR);
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.context).setWikiId("wiki");
        verify(this.context).setDoc(this.doc);
        verify(this.contextManager).popContext();
        verify(this.configurationSource).getProperty("defaultStyle");
    }

    @Test
    void getForceDefaultStyle() throws Exception
    {
        this.configuration.getForceDefaultStyle("wiki", DOCUMENT_REFERENCE_STR);
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.context).setWikiId("wiki");
        verify(this.context).setDoc(this.doc);
        verify(this.contextManager).popContext();
        verify(this.configurationSource).getProperty("forceDefaultStyle");
    }

    @Test
    void getDefaultStyleDocNull() throws Exception
    {
        this.configuration.getDefaultStyle("wiki", null);
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.context).setWikiId("wiki");
        verify(this.contextManager).popContext();
        verify(this.context, never()).setDoc(any());
    }

    @Test
    void getDefaultStylePushContextException() throws Exception
    {
        doThrow(ExecutionContextException.class).when(this.contextManager)
            .pushContext(any(ExecutionContext.class), anyBoolean());
        ImageStyleException exception =
            assertThrows(ImageStyleException.class, () -> this.configuration.getDefaultStyle("wiki", null));
        assertEquals("Failed to initialize the execution context", exception.getMessage());
        assertEquals(ExecutionContextException.class, exception.getCause().getClass());
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.contextManager).popContext();
    }

    @Test
    void getDefaultStyleGetDocumentException() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.context)).thenThrow(XWikiException.class);
        ImageStyleException exception =
            assertThrows(ImageStyleException.class,
                () -> this.configuration.getDefaultStyle("wiki", DOCUMENT_REFERENCE_STR));
        assertEquals("Failed to resolved document [xwiki:Space.Page]", exception.getMessage());
        assertEquals(XWikiException.class, exception.getCause().getClass());
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(false));
        verify(this.contextManager).popContext();
    }
}
