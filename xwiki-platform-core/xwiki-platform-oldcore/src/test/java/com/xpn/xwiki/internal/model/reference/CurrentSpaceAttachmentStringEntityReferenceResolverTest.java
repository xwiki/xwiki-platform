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
package com.xpn.xwiki.internal.model.reference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Unit tests for {@link CurrentSpaceAttachmentStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 7.4.1, 8.0M1
 */
@ComponentList({DefaultEntityReferenceProvider.class, CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class})
public class CurrentSpaceAttachmentStringEntityReferenceResolverTest
{
    public MockitoComponentMockingRule<CurrentSpaceAttachmentStringEntityReferenceResolver> mocker =
        new MockitoComponentMockingRule<>(CurrentSpaceAttachmentStringEntityReferenceResolver.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    private static final String CURRENT_WIKI = "currentwiki";

    private static final String CURRENT_SPACE = "currentdocspace";

    private static final String CURRENT_PAGE = "currentdocpage";

    private static final String DEFAULT_DOCUMENT_NAME = "WebHome";

    @Before
    public void setup() throws ComponentLookupException
    {
        this.oldcore.getXWikiContext().setWikiId(CURRENT_WIKI);
    }

    @Test
    public void testResolveTopLevelSpace() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result = mocker.getComponentUnderTest().resolve("Space@file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_WIKI, result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("Space", result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }

    @Test
    public void testResolveNestedSpace() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result =
            mocker.getComponentUnderTest().resolve("Space1.Space2@file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_WIKI, result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("Space2", result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("Space1", result.extractReference(EntityType.SPACE).getParent().getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }

    @Test
    public void testResolveAttachmentName() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result = mocker.getComponentUnderTest().resolve("file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_WIKI, result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals(CURRENT_SPACE, result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }

    @Test
    public void testResolveAbsoluteReference() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result = mocker.getComponentUnderTest().resolve("wiki:Space@file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals("wiki", result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("Space", result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }

    @Test
    public void testResolveSpaceNamedWebHome() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result = mocker.getComponentUnderTest().resolve("WebHome@file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_WIKI, result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("WebHome", result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }

    @Test
    public void testResolveNestedSpaceNamedWebHome() throws Exception
    {
        this.oldcore.getXWikiContext().setDoc(
            new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

        EntityReference result =
            mocker.getComponentUnderTest().resolve("Space.WebHome@file.ext", EntityType.ATTACHMENT);

        Assert.assertEquals(CURRENT_WIKI, result.extractReference(EntityType.WIKI).getName());
        Assert.assertEquals("WebHome", result.extractReference(EntityType.SPACE).getName());
        Assert.assertEquals("Space", result.extractReference(EntityType.SPACE).getParent().getName());
        Assert.assertEquals(DEFAULT_DOCUMENT_NAME, result.extractReference(EntityType.DOCUMENT).getName());
        Assert.assertEquals("file.ext", result.getName());
    }
}
