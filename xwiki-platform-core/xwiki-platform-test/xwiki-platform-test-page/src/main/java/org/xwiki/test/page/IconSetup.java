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
package org.xwiki.test.page;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Setup an icon environment for the page tests.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
public final class IconSetup
{
    private IconSetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Sets up an icon environment so that all icon services returns a result.
     *
     * @param pageTest the page test instance that needs to be setup
     * @param iconSetPath path of the icon set to use for the test (for instance, {@code "/icons/default.iconset"}) 
     * @throws XWikiException when an error occurs when accessing a document
     * @throws ComponentLookupException when an error occurs when resolving a component
     * @throws IOException when an error occurs when loading the icon page content from the resources
     */
    public static void setUp(PageTest pageTest, String iconSetPath)
        throws XWikiException, ComponentLookupException, IOException
    {
        String xwiki = "xwiki";
        XWikiDocument document =
            pageTest.xwiki.getDocument(new DocumentReference(xwiki, "Fake", "IconTheme"), pageTest.context);
        document.setContent(IOUtils.toString(IconSetup.class.getResourceAsStream(iconSetPath), UTF_8));
        BaseObject iconThemeObject = new BaseObject();
        iconThemeObject.setXClassReference(new DocumentReference(xwiki, "IconThemesCode", "IconThemeClass"));
        iconThemeObject.setStringValue("name", "FakeIconTheme");
        document.addXObject(iconThemeObject);
        pageTest.xwiki.saveDocument(document, pageTest.context);

        pageTest.oldcore.getMocker().<MemoryConfigurationSource>getInstance(ConfigurationSource.class, "all")
            .setProperty("iconTheme", "Fake.IconTheme");
    }
}
