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
package com.xpn.xwiki.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentRecycleBinStore;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentVersioningStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the deprecated store constructors re-added by
 * {@code StoreConstructorsCompatibilityAspect}.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@SuppressWarnings("deprecation")
class StoreConstructorsCompatibilityTest
{
    private static final String CONFIGURED_PATH = "/WEB-INF/custom-hibernate.cfg.xml";

    private static final String EXPLICIT_PATH = "/explicit/hibernate.cfg.xml";

    private XWiki xwiki;

    private XWikiContext context;

    @BeforeEach
    void setUp()
    {
        this.xwiki = mock(XWiki.class);
        when(this.xwiki.Param("xwiki.store.hibernate.path", "/WEB-INF/hibernate.cfg.xml"))
            .thenReturn(CONFIGURED_PATH);

        this.context = mock(XWikiContext.class);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void baseStoreTakesThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new XWikiHibernateBaseStore(this.xwiki, this.context).getPath());
    }

    @Test
    void baseStoreTakesTheExplicitPath()
    {
        assertEquals(EXPLICIT_PATH, new XWikiHibernateBaseStore(EXPLICIT_PATH).getPath());
    }

    @Test
    void storeTakesThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new XWikiHibernateStore(this.xwiki, this.context).getPath());
        assertEquals(CONFIGURED_PATH, new XWikiHibernateStore(this.context).getPath());
        assertEquals(EXPLICIT_PATH, new XWikiHibernateStore(EXPLICIT_PATH).getPath());
    }

    @Test
    void attachmentStoreTakesThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new XWikiHibernateAttachmentStore(this.xwiki, this.context).getPath());
        assertEquals(CONFIGURED_PATH, new XWikiHibernateAttachmentStore(this.context).getPath());
        assertEquals(EXPLICIT_PATH, new XWikiHibernateAttachmentStore(EXPLICIT_PATH).getPath());
    }

    @Test
    void versioningStoreTakesThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new XWikiHibernateVersioningStore(this.xwiki, this.context).getPath());
        assertEquals(CONFIGURED_PATH, new XWikiHibernateVersioningStore(this.context).getPath());
        assertEquals(EXPLICIT_PATH, new XWikiHibernateVersioningStore(EXPLICIT_PATH).getPath());
    }

    @Test
    void recycleBinStoresTakeThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new XWikiHibernateRecycleBinStore(this.context).getPath());
        assertEquals(CONFIGURED_PATH, new HibernateAttachmentRecycleBinStore(this.context).getPath());
    }

    @Test
    void attachmentVersioningStoreTakesThePathFromTheConfiguration()
    {
        assertEquals(CONFIGURED_PATH, new HibernateAttachmentVersioningStore(this.context).getPath());
    }

    @Test
    void voidAttachmentVersioningStoreIgnoresTheContext()
    {
        assertNotNull(new VoidAttachmentVersioningStore(this.context));
    }
}
