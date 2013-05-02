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
package org.xwiki.security.authorization.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.XWikiDocument;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;


/**
 * Test the bridge component for resolving content author on behalf of the AuthorizationContextFactory.
 *
 * @version $Id$
 * @since 4.3M1
 */
@RunWith(JMock.class)
public class DefaultContentAuthorResolverTest
{

    private final SpaceReference spaceReference = new SpaceReference("space", new WikiReference("wiki"));

    private final DocumentReference user1 = new DocumentReference("user1", spaceReference);

    private final Mockery mockery = new JUnit4Mockery();

    @Before
    public void setupLegacy() throws Exception
    {
        mockery.setImposteriser(ClassImposteriser.INSTANCE);
    }

    @Test
    public void authorResolver()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
        }});

        ContentAuthorResolver resolver = new DefaultContentAuthorResolver();

        Assert.assertTrue(user1.equals(resolver.resolveContentAuthor(doc)));
        
    }

    @Test
    public void contentDirty()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(true));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
        }});


        ContentAuthorResolver resolver = new DefaultContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(doc));
        
    }

    @Test
    public void newDocument()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(true));
        }});


        ContentAuthorResolver resolver = new DefaultContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(doc));
        
    }

    @Test
    public void nullAuthor()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getContentAuthorReference(); will(returnValue(null));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
        }});


        ContentAuthorResolver resolver = new DefaultContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(doc));
        
    }

    @Test
    public void nullDocument()
    {
        ContentAuthorResolver resolver = new DefaultContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(null));
    }

}
