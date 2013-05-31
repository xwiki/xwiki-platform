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
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;

import java.util.Collections;


/**
 * Test the conservative variant of the bridge component for resolving content author on behalf of the
 * AuthorizationContextFactory.
 *
 * @version $Id$
 * @since 4.3M1
 */
@RunWith(JMock.class)
public class ConservativeContentAuthorResolverTest
{

    private final WikiReference wikiReference = new WikiReference("wiki");

    private final SpaceReference spaceReference = new SpaceReference("space", wikiReference);

    private final SpaceReference wikiSpace = new SpaceReference(XWikiConstants.XWIKI_SPACE, wikiReference);

    private final DocumentReference requiredRight = new DocumentReference(XWikiConstants.REQUIRED_RIGHT_CLASSNAME,
                                                                          wikiSpace);

    private final DocumentReference user1 = new DocumentReference("user1", spaceReference);

    private final Mockery mockery = new JUnit4Mockery();

    @Before
    public void setupLegacy() throws Exception
    {
        mockery.setImposteriser(ClassImposteriser.INSTANCE);
    }

    @Test
    public void authorResolverNotMarked()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getDocumentReference();      will(returnValue(new DocumentReference("doc", spaceReference)));
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
            allowing(doc).getXObjects(with(equal(requiredRight))); will(returnValue(Collections.<BaseObject>emptyList()));
        }});

        ContentAuthorResolver resolver = new ConservativeContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(doc));
    }

    @Test
    public void authorResolverMarkedEdit()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        final BaseObject obj = mockery.mock(BaseObject.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getDocumentReference();      will(returnValue(new DocumentReference("doc", spaceReference)));
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
            allowing(doc).getXObjects(with(equal(requiredRight)));
            will(returnValue(Collections.<BaseObject>singletonList(obj)));
            allowing(obj).getStringValue("level");     will(returnValue("edit"));
        }});

        ContentAuthorResolver resolver = new ConservativeContentAuthorResolver();

        Assert.assertNull(resolver.resolveContentAuthor(doc));
        
    }

    @Test
    public void authorResolverMarkedProgramming()
    {
        final XWikiDocument doc = mockery.mock(XWikiDocument.class);

        final BaseObject obj = mockery.mock(BaseObject.class);

        mockery.checking(new Expectations() {{
            allowing(doc).getDocumentReference();      will(returnValue(new DocumentReference("doc", spaceReference)));
            allowing(doc).getContentAuthorReference(); will(returnValue(user1));
            allowing(doc).isContentDirty();            will(returnValue(false));
            allowing(doc).isMetaDataDirty();           will(returnValue(false));
            allowing(doc).isNew();                     will(returnValue(false));
            allowing(doc).getXObjects(with(equal(requiredRight)));
            will(returnValue(Collections.<BaseObject>singletonList(obj)));
            allowing(obj).getStringValue("level");     will(returnValue("programming"));
        }});

        ContentAuthorResolver resolver = new ConservativeContentAuthorResolver();

        Assert.assertEquals(user1, resolver.resolveContentAuthor(doc));
        
    }


}
