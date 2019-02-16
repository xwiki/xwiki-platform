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
package org.xwiki.mail.internal.configuration;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.mail.MailGeneralConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
public class GeneralMailConfigClassDocumentConfigurationSourceTest
{
    @InjectMockComponents(role = MailGeneralConfiguration.class)
    private GeneralMailConfigClassDocumentConfigurationSource configuration;

    @InjectComponentManager
    private ComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    protected CacheManager cacheManager;

    @MockComponent
    protected ConverterManager converter;

    @MockComponent
    @Named("wiki")
    private ConfigurationSource xwikiConfigSource;

    private LocalDocumentReference configClass;

    private XWikiDocument configPage;

    private BaseProperty<?> property;

    @BeforeEach
    private void mockPageObjectsSetup() throws Exception
    {
        configClass = new LocalDocumentReference("Mail", "GeneralMailConfigClass");

        property = mock(BaseProperty.class);

        configPage = mock(XWikiDocument.class);

        DocumentReference documentReference = new DocumentReference("wiki", "Mail", "MailConfig");
        XWiki xwiki = mock(XWiki.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(configPage);

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(wikiManager.getCurrentWikiId()).thenReturn("wiki");

        // mock the converter to return the original value always
        when(converter.convert(any(), any())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });

        @SuppressWarnings("unchecked")
        Cache<Object> cache = mock(Cache.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        when(xcontextProvider.get()).thenReturn(xcontext);

        configuration.initialize();
    }

    private void createGeneralConfig()
    {
        BaseObject object = mock(BaseObject.class);
        when(object.getField("obfuscate")).thenReturn(property);
        when(configPage.getXObject(configClass)).thenReturn(object);
    }

    @Test
    public void getDefaultObfuscatePropertyIfNotConfigured()
    {
        // test
        assertEquals(0, this.configuration.getProperty("obfuscate", 0));
        assertEquals(1, this.configuration.getProperty("obfuscate", 1));
        assertFalse(this.configuration.isObfuscateEmails());
    }

    @Test
    public void getObfuscatePropertyIfSet()
    {
        createGeneralConfig();
        // set a mock value
        when(property.getValue()).thenReturn(1);

        // test
        assertEquals(1, this.configuration.getProperty("obfuscate", 0));
        assertTrue(this.configuration.isObfuscateEmails());
    }

    @Test
    public void getObfuscatePropertyFromXWikiPreferencesOnlyIfGeneralConfigIsMissing()
    {
        // set property in XWikiPreferences
        when(xwikiConfigSource.getProperty("obfuscateEmailAddresses", Integer.class)).thenReturn(1);

        // test
        assertEquals(1, this.configuration.getProperty("obfuscate", 0));
        assertTrue(this.configuration.isObfuscateEmails());

        createGeneralConfig();
        when(property.getValue()).thenReturn(0);
        assertEquals(0, this.configuration.getProperty("obfuscate", 1));
        assertFalse(this.configuration.isObfuscateEmails());
    }

}