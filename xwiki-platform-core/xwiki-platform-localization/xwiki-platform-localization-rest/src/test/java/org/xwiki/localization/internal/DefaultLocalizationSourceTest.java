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
package org.xwiki.localization.internal;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xpn.xwiki.XWikiContext;

import ch.qos.logback.classic.Level;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultLocalizationSource}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class DefaultLocalizationSourceTest
{
    @InjectMockComponents
    private DefaultLocalizationSource defaultLocalizationSource;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private ComponentManager componentManager;

    @Mock
    private LocalizationContext localizationContext;

    @Mock
    private LocalizationManager localizationManager;

    private final Locale defaultLocale = Locale.forLanguageTag("es");

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWikiId()).thenReturn("initialWikiId");
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
        when(this.componentManager.getInstance(LocalizationContext.class)).thenReturn(this.localizationContext);
        when(this.componentManager.getInstance(LocalizationManager.class)).thenReturn(this.localizationManager);
        when(localizationContext.getCurrentLocale()).thenReturn(this.defaultLocale);
    }

    @Test
    void translations()
    {
        Translation translationKey1 = mock(Translation.class);
        Translation translationKey2 = mock(Translation.class);
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("key1", "value1");
        expected.put("key2", "value2");

        when(localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(translationKey1);
        when(localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(translationKey2);
        when(translationKey1.getRawSource()).thenReturn("value1");
        when(translationKey2.getRawSource()).thenReturn("value2");

        Response response =
            this.defaultLocalizationSource.translations("mywiki", null, null, Arrays.asList("key1", "key2"));

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(expected, response.getEntity());

        verify(localizationManager).getTranslation("key1", this.defaultLocale);
        verify(localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.xcontext).setWikiId("mywiki");
        verify(this.xcontext).setWikiId("initialWikiId");
    }

    @Test
    void translationsComponentLookupFail() throws Exception
    {
        when(this.componentManager.getInstance(LocalizationContext.class)).thenThrow(ComponentLookupException.class);

        Response response =
            this.defaultLocalizationSource.translations("mywiki", null, null, Arrays.asList("key1", "key2"));

        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(ComponentLookupException.class, response.getEntity().getClass());

        // Checks the wiki id is restored even in case of failure.
        verify(this.xcontext).setWikiId("initialWikiId");
    }

    @Test
    void translationsNotFound()
    {
        Translation translationKey2 = mock(Translation.class);
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.putNull("key1");
        expected.put("key2", "value2");

        when(localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(null);
        when(localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(translationKey2);
        when(translationKey2.getRawSource()).thenReturn("value2");

        Response response =
            this.defaultLocalizationSource.translations("mywiki", null, null, Arrays.asList("key1", "key2"));

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(expected, response.getEntity());

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Key [key1] not found for local [es] in wiki [mywiki].", this.logCapture.getMessage(0));

        verify(localizationManager).getTranslation("key1", this.defaultLocale);
        verify(localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.xcontext).setWikiId("mywiki");
        verify(this.xcontext).setWikiId("initialWikiId");
    }

    @Test
    void translationsWithLocale()
    {
        Translation translationKey1 = mock(Translation.class);
        Translation translationKey2 = mock(Translation.class);
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("key1", "value1");
        expected.put("key2", "value2");

        Locale localeFr = Locale.forLanguageTag("fr");
        when(localizationManager.getTranslation("key1", localeFr)).thenReturn(translationKey1);
        when(localizationManager.getTranslation("key2", localeFr)).thenReturn(translationKey2);
        when(translationKey1.getRawSource()).thenReturn("value1");
        when(translationKey2.getRawSource()).thenReturn("value2");

        Response response =
            this.defaultLocalizationSource.translations("mywiki", "fr", null, Arrays.asList("key1", "key2"));

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(expected, response.getEntity());

        verify(localizationManager).getTranslation("key1", localeFr);
        verify(localizationManager).getTranslation("key2", localeFr);
        verify(this.xcontext).setWikiId("mywiki");
        verify(this.xcontext).setWikiId("initialWikiId");
    }

    @Test
    void translationWithPrefix()
    {
        Translation translationKey1 = mock(Translation.class);
        Translation translationKey2 = mock(Translation.class);
        ObjectNode expected = JsonNodeFactory.instance.objectNode();
        expected.put("key1", "value1");
        expected.put("key2", "value2");

        when(localizationManager.getTranslation("key1", this.defaultLocale)).thenReturn(translationKey1);
        when(localizationManager.getTranslation("key2", this.defaultLocale)).thenReturn(translationKey2);
        when(translationKey1.getRawSource()).thenReturn("value1");
        when(translationKey2.getRawSource()).thenReturn("value2");

        Response response =
            this.defaultLocalizationSource.translations("mywiki", null, "key", Arrays.asList("1", "2"));

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(expected, response.getEntity());

        verify(localizationManager).getTranslation("key1", defaultLocale);
        verify(localizationManager).getTranslation("key2", this.defaultLocale);
        verify(this.xcontext).setWikiId("mywiki");
        verify(this.xcontext).setWikiId("initialWikiId");
    }
}