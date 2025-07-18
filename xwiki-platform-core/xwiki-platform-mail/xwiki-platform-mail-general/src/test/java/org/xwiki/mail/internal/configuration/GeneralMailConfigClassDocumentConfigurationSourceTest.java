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

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeneralMailConfigClassDocumentConfigurationSource}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
class GeneralMailConfigClassDocumentConfigurationSourceTest
{
    @InjectMockComponents
    private GeneralMailConfigClassDocumentConfigurationSource source;

    @MockComponent
    private ConverterManager converterManager;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @BeforeComponent
    public void before() throws Exception
    {
        Cache<Object> cache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);
    }

    @Test
    void getPropertyWhenSendMailConfigClassXObjectExists() throws Exception
    {
        when(this.converterManager.convert(String.class, "value")).thenReturn("value");

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");

        LocalDocumentReference classReference = new LocalDocumentReference("Mail", "GeneralMailConfigClass");

        BaseProperty property = mock(BaseProperty.class);
        when(property.toText()).thenReturn("value");

        BaseObject object = mock(BaseObject.class);
        when(object.getField("key")).thenReturn(property);

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getXObject(classReference)).thenReturn(object);

        DocumentReference documentReference = new DocumentReference("wiki", "Mail", "MailConfig");
        XWiki xwiki = mock(XWiki.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(document);

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(this.xcontextProvider.get()).thenReturn(xcontext);

        assertEquals("value", this.source.getProperty("key", "defaultValue"));
    }

    @Test
    void getPropertyWhenNoSendMailConfigClassXObject() throws Exception
    {
        Cache<Object> cache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");

        LocalDocumentReference classReference = new LocalDocumentReference("Mail", "GeneralMailConfigClass");

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getXObject(classReference)).thenReturn(null);

        DocumentReference documentReference = new DocumentReference("wiki", "Mail", "MailConfig");
        XWiki xwiki = mock(XWiki.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(document);

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(this.xcontextProvider.get()).thenReturn(xcontext);

        assertEquals("defaultValue", this.source.getProperty("key", "defaultValue"));
    }
}
