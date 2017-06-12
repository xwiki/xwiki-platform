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
package org.xwiki.component.wiki;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentManagerRegistrationHelper;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.component.wiki.internal.bridge.WikiObjectComponentManagerRegistererProxy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiObjectComponentManagerRegistererProxy}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class WikiObjectComponentManagerRegistererProxyTest
{
    @Rule
    public MockitoComponentMockingRule<WikiObjectComponentManagerRegistererProxy> mocker =
            new MockitoComponentMockingRule<>(WikiObjectComponentManagerRegistererProxy.class);

    private ComponentManager componentManager;

    private WikiComponentManagerRegistrationHelper wikiComponentManagerRegistrationHelper;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Before
    public void setUp() throws Exception
    {
        this.componentManager = this.mocker.registerMockComponent(ComponentManager.class);
        this.wikiComponentManagerRegistrationHelper =
                this.mocker.registerMockComponent(WikiComponentManagerRegistrationHelper.class);
        this.entityReferenceSerializer =
                this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
    }

    @Test
    public void testWikiObjectListCollection() throws Exception
    {
        WikiObjectComponentBuilder builder1 = mock(WikiObjectComponentBuilder.class);
        WikiObjectComponentBuilder builder2 = mock(WikiObjectComponentBuilder.class);

        EntityReference builder1Reference = new EntityReference("builder1", EntityType.OBJECT);
        EntityReference builder2Reference = new EntityReference("builder2", EntityType.OBJECT);

        when(builder1.getClassReference()).thenReturn(builder1Reference);
        when(builder2.getClassReference()).thenReturn(builder2Reference);

        when(this.componentManager.getInstanceList(WikiObjectComponentBuilder.class))
                .thenReturn(Arrays.asList(builder1, builder2));

        this.mocker.getComponentUnderTest().collectWikiObjectsList();

        assertEquals(2, this.mocker.getComponentUnderTest().getWikiObjectsList().size());
    }

    @Test
    public void testRegisterObjectComponentsWithStandardComponentBuilder() throws Exception
    {
        BaseObjectReference objectReference = mock(BaseObjectReference.class);
        XWikiDocument source = mock(XWikiDocument.class);
        WikiObjectComponentBuilder componentBuilder = mock(WikiObjectComponentBuilder.class);

        this.testWithWikiObjectComponentBuilder(componentBuilder, objectReference, source);

        verify(componentBuilder, times(1)).buildComponents(objectReference);
    }

    @Test
    public void testRegisterObjectComponentsWithBaseObjectComponentBuilder() throws Exception
    {
        BaseObjectReference objectReference = mock(BaseObjectReference.class);
        XWikiDocument source = mock(XWikiDocument.class);
        WikiBaseObjectComponentBuilder componentBuilder = mock(WikiBaseObjectComponentBuilder.class);

        this.testWithWikiObjectComponentBuilder(componentBuilder, objectReference, source);

        verify(componentBuilder, times(1)).buildComponents(any(BaseObject.class));
    }

    @Test
    public void testUnregisterObjectComponents() throws Exception
    {
        EntityReference testReference = new EntityReference("test", EntityType.OBJECT);
        this.mocker.getComponentUnderTest().unregisterObjectComponents(testReference);

        verify(this.wikiComponentManagerRegistrationHelper, times(1))
                .unregisterComponents(testReference);
    }

    private void testWithWikiObjectComponentBuilder(WikiObjectComponentBuilder buider,
            BaseObjectReference objectReference, XWikiDocument source) throws Exception
    {
        DocumentReference objectXClassReference = mock(DocumentReference.class);

        when(source.getXObject(any(EntityReference.class))).thenReturn(mock(BaseObject.class));
        when(objectReference.getXClassReference()).thenReturn(objectXClassReference);
        when(this.entityReferenceSerializer.serialize(any())).thenReturn("referencePath");
        when(this.componentManager.getInstance(WikiObjectComponentBuilder.class, "referencePath"))
                .thenReturn(buider);

        this.mocker.getComponentUnderTest().registerObjectComponents(objectReference, source);
    }

}
