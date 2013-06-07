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
package com.xpn.xwiki.plugin.wikimanager;

import static org.mockito.Mockito.*;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.plugin.packaging.PackageAPI;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link WikiCopy}.
 * 
 * @version $Id$
 */
public class WikiCopyTest
{
    /**
     * A component manager that allows us to register mock components.
     */
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    /**
     * The instance being tested.
     */
    private WikiCopy wikiCopy;

    /**
     * Mock {@link XWikiContext}.
     */
    private XWikiContext context = mock(XWikiContext.class);

    /**
     * Test setup.
     * 
     * @throws Exception if test setup fails
     */
    @Before
    public void setUp() throws Exception
    {
        mocker.registerMockComponent(ContextualLocalizationManager.class);
        Utils.setComponentManager(mocker);
        wikiCopy = new WikiCopy();

        XWiki wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(wiki);
    }

    @Test
    public void copyWiki() throws Exception
    {
        String currentWiki = "carol";
        when(context.getDatabase()).thenReturn(currentWiki);

        String sourceWiki = "alice";
        String targetWiki = "bob";
        wikiCopy.copyWiki(sourceWiki, targetWiki, "Copy wiki", context);

        verify(context.getWiki()).copyWiki(sourceWiki, targetWiki, null, context);
        verify(context).setDatabase(targetWiki);
        verify(context).setDatabase(currentWiki);
    }

    @Test
    public void importPackage() throws Exception
    {
        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(context.getDoc()).thenReturn(currentDocument);

        String packageFileName = "foo.xar";
        XWikiAttachment packageFile = mock(XWikiAttachment.class);
        when(currentDocument.getAttachment(packageFileName)).thenReturn(packageFile);

        InputStream packageContent = mock(InputStream.class);
        when(packageFile.getContentInputStream(context)).thenReturn(packageContent);

        PackageAPI importer = mock(PackageAPI.class);
        when(context.getWiki().getPluginApi("package", context)).thenReturn(importer);
        when(importer.install()).thenReturn(DocumentInfo.INSTALL_OK);

        String currentWiki = "test";
        when(context.getDatabase()).thenReturn(currentWiki);

        String targetWiki = "bar";
        wikiCopy.importPackage(packageFileName, targetWiki, context);

        verify(importer).Import(packageContent);
        verify(context).setDatabase(targetWiki);
        verify(context).setDatabase(currentWiki);
    }
}
