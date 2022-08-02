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
package org.xwiki.user.internal.document;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link NormalUserPreferencesConfigurationSource}.
 *
 * @version $Id$
 */
@ComponentTest
public class NormalUserPreferencesConfigurationSourceTest
{
    @InjectMockComponents
    private NormalUserPreferencesConfigurationSource source;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    protected ConverterManager converter;

    @BeforeComponent
    public void setup() throws Exception
    {
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(mock(Cache.class));
    }

    @Test
    void getProperty() throws Exception
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        when(this.dab.getCurrentUserReference()).thenReturn(userDocumentReference);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument document = mock(XWikiDocument.class);
        LocalDocumentReference classReference = new LocalDocumentReference("XWiki", "XWikiUsers");
        BaseObject baseObject = mock(BaseObject.class);
        BaseProperty property = mock(BaseProperty.class);
        when(property.toText()).thenReturn("value");
        when(baseObject.getField("key")).thenReturn(property);
        when(document.getXObject(classReference)).thenReturn(baseObject);
        when(xwiki.getDocument(userDocumentReference, xcontext)).thenReturn(document);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(this.contextProvider.get()).thenReturn(xcontext);
        when(this.converter.convert(String.class, "value")).thenReturn("value");

        assertEquals("value", this.source.getProperty("key", String.class));
    }
}
