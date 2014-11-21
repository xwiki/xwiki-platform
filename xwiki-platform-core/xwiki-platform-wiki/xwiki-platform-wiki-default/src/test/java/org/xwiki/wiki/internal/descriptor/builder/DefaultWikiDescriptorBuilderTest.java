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
package org.xwiki.wiki.internal.descriptor.builder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;
import org.xwiki.wiki.internal.descriptor.properties.WikiPropertyGroupManager;
import org.xwiki.wiki.properties.WikiPropertyGroupException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.descriptor.builder.DefaultWikiDescriptorBuilder}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultWikiDescriptorBuilderTest
{
    @Rule
    public org.xwiki.test.mockito.MockitoComponentMockingRule<DefaultWikiDescriptorBuilder> mocker =
            new MockitoComponentMockingRule(DefaultWikiDescriptorBuilder.class);

    private Provider<XWikiContext> xcontextProvider;

    private EntityReferenceSerializer<String> referenceSerializer;

    private DocumentReferenceResolver<String> referenceResolver;

    private WikiDescriptorManager wikiDescriptorManager;

    private WikiPropertyGroupManager wikiPropertyGroupManager;

    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    private XWikiContext context;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        context = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(context);
        xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        referenceSerializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        referenceResolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);

        wikiDescriptorManager = mock(WikiDescriptorManager.class);
        wikiPropertyGroupManager = mock(WikiPropertyGroupManager.class);
        wikiDescriptorDocumentHelper = mocker.getInstance(WikiDescriptorDocumentHelper.class);

        Provider<WikiDescriptorManager> wikiDescriptorManagerProvider = mocker.getInstance(
                new DefaultParameterizedType(null, Provider.class, WikiDescriptorManager.class));
        Provider<WikiPropertyGroupManager> wikiPropertyGroupManagerProvider = mocker.getInstance(
                new DefaultParameterizedType(null, Provider.class, WikiPropertyGroupManager.class));

        when(wikiDescriptorManagerProvider.get()).thenReturn(wikiDescriptorManager);
        when(wikiPropertyGroupManagerProvider.get()).thenReturn(wikiPropertyGroupManager);
    }

    @Test
    public void buildDescriptorObject() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        BaseObject object2 = mock(BaseObject.class);
        BaseObject object3 = mock(BaseObject.class);
        // Make sure that the first object is null to also verify this case since it can happen that we get holes
        // with the XWikiDocument.getXObjects() API...
        objects.add(null);
        objects.add(object1);
        objects.add(object2);
        objects.add(null);
        objects.add(object3);

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(document.getDocumentReference()).thenReturn(documentReference);

        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("subwiki1");
        when(object2.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("alias1");
        when(object3.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("alias2");

        DocumentReference mainPageReference = new DocumentReference("subwiki1", "Space", "MainPage");

        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE)).thenReturn("Space.MainPage");
        when(referenceResolver.resolve("Space.MainPage")).
                thenReturn(mainPageReference);
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME)).
                thenReturn("myPrettyName");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_OWNER)).
                thenReturn("myOwner");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_DESCRIPTION)).
                thenReturn("myDescription");

        DocumentReference ownerRef = new DocumentReference("xwiki", "XWiki", "myOwner");
        when(referenceResolver.resolve("myOwner")).thenReturn(ownerRef);
        when(referenceSerializer.serialize(ownerRef)).thenReturn("xwiki:XWiki.myOwner");

        // Test
        WikiDescriptor result = mocker.getComponentUnderTest().buildDescriptorObject(objects, document);

        assertEquals("subwiki1", result.getId());
        assertEquals(3, result.getAliases().size());
        assertEquals("subwiki1", result.getAliases().get(0));
        assertEquals("alias1", result.getAliases().get(1));
        assertEquals("alias2", result.getAliases().get(2));
        assertEquals(mainPageReference, result.getMainPageReference());
        assertEquals("myPrettyName", result.getPrettyName());
        assertEquals("xwiki:XWiki.myOwner", result.getOwnerId());
        assertEquals("myDescription", result.getDescription());

        // Verify
        wikiPropertyGroupManager.loadForDescriptor(any(WikiDescriptor.class));
    }

    @Test
    public void buildDescriptorObjectWhenInvalidWiki() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        objects.add(object1);
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn(" ");

        XWikiDocument document = mock(XWikiDocument.class);

        // Test
        WikiDescriptor result = mocker.getComponentUnderTest().buildDescriptorObject(objects, document);
        assertNull(result);
    }

    @Test
    public void buildDescriptorObjectWhenException() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        objects.add(object1);

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("subwiki1");

        DocumentReference mainPageReference = new DocumentReference("subwiki1", "Space", "MainPage");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE)).thenReturn("Space.MainPage");
        when(referenceResolver.resolve("Space.MainPage")).
                thenReturn(mainPageReference);

        Exception exception = new WikiPropertyGroupException("error in wikiPropertyGroupManager.loadForDescriptor");
        doThrow(exception).when(wikiPropertyGroupManager).loadForDescriptor(any(WikiDescriptor.class));

        // Test
        mocker.getComponentUnderTest().buildDescriptorObject(objects, document);

        // Verify
        verify(mocker.getMockedLogger()).error("Failed to load wiki property groups for wiki [{}].",
                "subwiki1", exception);
    }
}
