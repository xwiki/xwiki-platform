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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroup;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;
import org.xwiki.wiki.template.WikiTemplatePropertyGroup;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link WikiTemplatePropertyGroupProvider}.
 *
 * @since 5.4.2
 * @version $Id$
 */
public class WikiTemplatePropertyGroupProviderTest
{
    @Rule
    public MockitoComponentMockingRule<WikiPropertyGroupProvider> mocker =
            new MockitoComponentMockingRule(WikiTemplatePropertyGroupProvider.class, WikiPropertyGroupProvider.class,
                    "template");

    private Provider<XWikiContext> xcontextProvider;

    private WikiDescriptorManager wikiDescriptorManager;

    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        wikiDescriptorDocumentHelper = mocker.getInstance(WikiDescriptorDocumentHelper.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void get() throws Exception
    {
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenReturn(descriptorDocument);
        BaseObject object = mock(BaseObject.class);
        when(descriptorDocument.getXObject(eq(WikiTemplateClassDocumentInitializer.SERVER_CLASS))).thenReturn(object);
        when(object.getIntValue("iswikitemplate", 0)).thenReturn(1);

        // Test
        WikiPropertyGroup result = mocker.getComponentUnderTest().get("wikiId");

        // Verify
        assertEquals(true, result.get("isTemplate"));
        assertTrue(result instanceof WikiTemplatePropertyGroup);
        assertTrue(((WikiTemplatePropertyGroup)result).isTemplate());

        XWikiDocument descriptorDocument2 = mock(XWikiDocument.class);
        when(wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId2")).thenReturn(descriptorDocument2);
        BaseObject object2 = mock(BaseObject.class);
        when(descriptorDocument2.getXObject(eq(WikiTemplateClassDocumentInitializer.SERVER_CLASS))).thenReturn(object2);
        when(object2.getIntValue("iswikitemplate", 0)).thenReturn(0);

        // Test
        WikiPropertyGroup result2 = mocker.getComponentUnderTest().get("wikiId2");

        // Verify
        assertEquals(false, result2.get("isTemplate"));
        assertTrue(result2 instanceof WikiTemplatePropertyGroup);
        assertFalse(((WikiTemplatePropertyGroup) result2).isTemplate());
    }

    @Test
    public void getWhenNoObject() throws Exception
    {
        XWikiDocument descriptorDocument = mock(XWikiDocument.class);
        when(wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenReturn(descriptorDocument);

        // Test
        WikiPropertyGroup result = mocker.getComponentUnderTest().get("wikiId");

        // Verify
        assertEquals(false, result.get("isTemplate"));
    }

    @Test
    public void getWhenException() throws Exception
    {
        WikiManagerException exception = new WikiManagerException("error in WikiManager");
        when(wikiDescriptorDocumentHelper.getDocumentFromWikiId("wikiId")).thenThrow(exception);

        // Test
        boolean exceptionCaught = false;
        try {
            mocker.getComponentUnderTest().get("wikiId");
        } catch(WikiPropertyGroupException e) {
            exceptionCaught = true;
            assertEquals("Unable to load descriptor document for wiki [wikiId].", e.getMessage());
            assertEquals(exception, e.getCause());
        }

        assertTrue(exceptionCaught);
    }

}
