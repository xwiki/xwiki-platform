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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.SendMailConfigClassDocumentConfigurationSource}.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class SendMailConfigClassDocumentConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<SendMailConfigClassDocumentConfigurationSource> mocker =
        new MockitoComponentMockingRule<>(SendMailConfigClassDocumentConfigurationSource.class);

    @Test
    public void getPropertyWhenSendMailConfigClassXObjectExists() throws Exception
    {
        ConverterManager converterManager = this.mocker.getInstance(ConverterManager.class);
        when(converterManager.convert(String.class, "value")).thenReturn("value");

        Cache<Object> cache = mock(Cache.class);
        CacheManager cacheManager = this.mocker.getInstance(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");

        LocalDocumentReference classReference = new LocalDocumentReference("Mail", "SendMailConfigClass");

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

        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);

        assertEquals("value", this.mocker.getComponentUnderTest().getProperty("key", "defaultValue"));
    }

    @Test
    public void getPropertyWhenNoSendMailConfigClassXObject() throws Exception
    {
        Cache<Object> cache = mock(Cache.class);
        CacheManager cacheManager = this.mocker.getInstance(CacheManager.class);
        when(cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(cache);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");

        LocalDocumentReference classReference = new LocalDocumentReference("Mail", "SendMailConfigClass");

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getXObject(classReference)).thenReturn(null);

        DocumentReference documentReference = new DocumentReference("wiki", "Mail", "MailConfig");
        XWiki xwiki = mock(XWiki.class);
        when(xwiki.getDocument(eq(documentReference), any(XWikiContext.class))).thenReturn(document);

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);

        assertEquals("defaultValue", this.mocker.getComponentUnderTest().getProperty("key", "defaultValue"));
    }
}
