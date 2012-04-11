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
package org.xwiki.url.internal.standard;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLType;
import org.xwiki.url.standard.XWikiURLBuilder;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for {@link EntityXWikiURLBuilder}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class EntityXWikiURLBuilderTest extends AbstractComponentTestCase
{
    private static final WikiReference WIKI_REFERENCE = new WikiReference("Wiki");

    private XWikiURLBuilder builder;

    @Override protected void registerComponents() throws Exception
    {
        this.builder = getComponentManager().getInstance(XWikiURLBuilder.class, "entity");
    }

    @Test
    public void testCreateXWikiURLWhenNoViewAction() throws Exception
    {
        XWikiURL xwikiURL = builder.build(WIKI_REFERENCE, Collections.<String>emptyList());
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Main", "WebHome"));

        xwikiURL = builder.build(WIKI_REFERENCE, Arrays.asList(""));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Main", "WebHome"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("Space", "Page"));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("Page"));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Main", "Page"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("Space", ""));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "WebHome"));
    }

    @Test
    public void testCreateXWikiURLWhenViewAction() throws Exception
    {
        XWikiURL xwikiURL = builder.build(WIKI_REFERENCE, Arrays.asList("view", "Space", ""));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "WebHome"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("view", "Space", "Page"));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("view", "Space", "Page", ""));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("view", "Space", "Page", "ignored", "path"));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

        // Ensure there can be dots in document name for example
        xwikiURL = builder.build(WIKI_REFERENCE,  Arrays.asList("view", "Space", "Page.With.Dots"));
        assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page.With.Dots"));
    }

    @Test
    public void testCreateXWikiURLWhenDownloadAction() throws Exception
    {
        XWikiURL xwikiURL = builder.build(WIKI_REFERENCE, Arrays.asList("download", "Space", "Page", "attachment.ext"));
        assertXWikiURL(xwikiURL, "download",
            new AttachmentReference("attachment.ext", new DocumentReference("Wiki", "Space", "Page")));
    }

    private void assertXWikiURL(XWikiURL xwikiURL, String expectedAction, EntityReference expectedReference)
        throws Exception
    {
        Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
        XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
        Assert.assertEquals(expectedAction, entityURL.getAction());
        Assert.assertEquals(expectedReference, entityURL.getEntityReference());
    }
}
