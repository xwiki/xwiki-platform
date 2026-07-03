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
package org.xwiki.wiki.template.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroup;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.template.WikiTemplatePropertyGroup;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link WikiTemplatePropertyGroupProvider}.
 *
 * @since 5.4.2
 * @version $Id$
 */
@ComponentTest
class WikiTemplatePropertyGroupProviderTest
{
    @InjectMockComponents
    private WikiTemplatePropertyGroupProvider provider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void get() throws Exception
    {
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(this.wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenReturn(descriptorDocument);
        BaseObject object = mock(BaseObject.class);
        when(descriptorDocument.getXObject(eq(WikiTemplateClassDocumentInitializer.SERVER_CLASS))).thenReturn(object);
        when(object.getIntValue("iswikitemplate", 0)).thenReturn(1);

        // Test
        WikiPropertyGroup result = this.provider.get("wikiId");

        // Verify
        assertEquals(true, result.get("isTemplate"));
        assertTrue(result instanceof WikiTemplatePropertyGroup);
        assertTrue(((WikiTemplatePropertyGroup)result).isTemplate());

        XWikiDocument descriptorDocument2 = mock(XWikiDocument.class);
        when(this.wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId2")).thenReturn(descriptorDocument2);
        BaseObject object2 = mock(BaseObject.class);
        when(descriptorDocument2.getXObject(eq(WikiTemplateClassDocumentInitializer.SERVER_CLASS))).thenReturn(object2);
        when(object2.getIntValue("iswikitemplate", 0)).thenReturn(0);

        // Test
        WikiPropertyGroup result2 = this.provider.get("wikiId2");

        // Verify
        assertEquals(false, result2.get("isTemplate"));
        assertTrue(result2 instanceof WikiTemplatePropertyGroup);
        assertFalse(((WikiTemplatePropertyGroup) result2).isTemplate());
    }

    @Test
    void getWhenNoObject() throws Exception
    {
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(this.wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenReturn(descriptorDocument);

        // Test
        WikiPropertyGroup result = this.provider.get("wikiId");

        // Verify
        assertEquals(false, result.get("isTemplate"));
    }

    @Test
    void getWhenException() throws Exception
    {
        WikiManagerException expectedException = new WikiManagerException("error in WikiManager");
        when(this.wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenThrow(expectedException);

        // Test
        Throwable exception = assertThrows(WikiPropertyGroupException.class, () -> {
            this.provider.get("wikiId");
        });
        assertEquals("Unable to load descriptor document for wiki [wikiId].", exception.getMessage());
        assertEquals(expectedException, exception.getCause());
    }

}
