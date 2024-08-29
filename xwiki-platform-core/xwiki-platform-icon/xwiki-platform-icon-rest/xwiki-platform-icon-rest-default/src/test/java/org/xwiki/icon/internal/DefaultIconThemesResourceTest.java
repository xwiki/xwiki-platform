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
package org.xwiki.icon.internal;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.rest.model.jaxb.Icon;
import org.xwiki.icon.rest.model.jaxb.Icons;
import org.xwiki.icon.rest.model.jaxb.ObjectFactory;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.icon.IconManager.META_DATA_ICON_SET_TYPE;

/**
 * Test of {@link DefaultIconThemesResource}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class DefaultIconThemesResourceTest
{
    private static final DocumentReference CURRENT_ENTITY =
        new DocumentReference("currentwiki", "XWiki", "CurrentEntity");

    @InjectMockComponents
    private DefaultIconThemesResource iconThemesResource;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private IconManager iconManager;

    @MockComponent
    private IconSetManager iconSetManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.modelContext.getCurrentEntityReference()).thenReturn(CURRENT_ENTITY);
        when(this.wikiDescriptorManager.exists(any(String.class))).thenReturn(true);
    }

    @Test
    void getIconsByThemeThemeNotFound()
    {
        List<String> names = singletonList(null);
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.iconThemesResource.getIconsByTheme("wikiTest", "testTheme", names));

        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());

        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getIconsByThemeNoNames() throws Exception
    {
        when(this.iconSetManager.getIconSet("testTheme")).thenReturn(new IconSet("testTheme"));

        Icons response = this.iconThemesResource.getIconsByTheme("wikiTest", "testTheme", singletonList(null));

        Icons expected = new ObjectFactory().createIcons();
        assertEquals(marshal(expected), marshal(response));
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getIconCurrentTheme() throws Exception
    {
        when(this.iconSetManager.getDefaultIconSet()).thenReturn(new IconSet("testTheme"));
        when(this.iconManager.hasIcon("testTheme", "plus")).thenReturn(true);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_DATA_ICON_SET_TYPE, "icon");
        when(this.iconManager.getMetaData("plus", "testTheme")).thenReturn(metadata);
        Icons response = this.iconThemesResource.getIcons("wikiTest", singletonList("plus"));

        ObjectFactory objectFactory = new ObjectFactory();
        Icons expected = objectFactory.createIcons();
        Icon icon = objectFactory.createIcon();
        icon.setName("plus");
        icon.setIconSetType("icon");
        icon.setIconSetName("testTheme");
        expected.getIcons().add(icon);
        assertEquals(marshal(expected), marshal(response));
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getIconsByTheme() throws Exception
    {
        Map<String, Object> iconAMetaData = new HashMap<>();
        iconAMetaData.put("iconSetName", "testTheme");
        iconAMetaData.put("iconSetType", "TYPETEST");
        iconAMetaData.put("url", "");
        iconAMetaData.put("cssClass", "testclass");
        Map<String, Object> iconBMetaData = new HashMap<>();
        iconBMetaData.put("iconSetName", "testTheme");
        iconBMetaData.put("iconSetType", "TYPETEST2");
        iconBMetaData.put("url", "http://test/url");
        iconBMetaData.put("cssClass", "");

        when(this.iconSetManager.getIconSet("testTheme")).thenReturn(new IconSet("testTheme"));
        when(this.iconManager.hasIcon("testTheme","iconA")).thenReturn(true);
        when(this.iconManager.hasIcon("testTheme", "iconB")).thenReturn(true);
        when(this.iconManager.getMetaData("iconA", "testTheme")).thenReturn(iconAMetaData);
        when(this.iconManager.getMetaData("iconB", "testTheme")).thenReturn(iconBMetaData);

        Icons response = this.iconThemesResource
            .getIconsByTheme("wikiTest", "testTheme", asList("iconA", "unknownIcon", "iconB"));

        ObjectFactory objectFactory = new ObjectFactory();
        Icons expected = objectFactory.createIcons();
        Icon iconA = objectFactory.createIcon();
        iconA.setName("iconA");
        iconA.setIconSetName("testTheme");
        iconA.setIconSetType("TYPETEST");
        iconA.setCssClass("testclass");
        expected.getIcons().add(iconA);
        Icon iconB = objectFactory.createIcon();
        iconB.setName("iconB");
        iconB.setIconSetName("testTheme");
        iconB.setIconSetType("TYPETEST2");
        iconB.setUrl("http://test/url");
        expected.getIcons().add(iconB);
        expected.getMissingIcons().add("unknownIcon");
        assertEquals(marshal(expected), marshal(response));
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
    }

    @Test
    void getIcons() throws Exception
    {
        Map<String, Object> iconAMetaData = new HashMap<>();
        iconAMetaData.put("iconSetName", "testTheme");
        iconAMetaData.put("iconSetType", "TYPETEST");
        iconAMetaData.put("url", "");
        iconAMetaData.put("cssClass", "testclass");
        Map<String, Object> iconBMetaData = new HashMap<>();
        iconBMetaData.put("iconSetName", "testTheme");
        iconBMetaData.put("iconSetType", "TYPETEST2");
        iconBMetaData.put("url", "http://test/url");
        iconBMetaData.put("cssClass", "");

        when(this.iconSetManager.getCurrentIconSet()).thenReturn(new IconSet("testTheme"));
        when(this.iconManager.hasIcon("testTheme", "iconA")).thenReturn(true);
        when(this.iconManager.hasIcon("testTheme", "iconB")).thenReturn(true);
        when(this.iconManager.getMetaData("iconA", "testTheme")).thenReturn(iconAMetaData);
        when(this.iconManager.getMetaData("iconB", "testTheme")).thenReturn(iconBMetaData);

        Icons response = this.iconThemesResource.getIcons("wikiTest", asList("iconA", "unknownIcon", "iconB"));

        ObjectFactory objectFactory = new ObjectFactory();
        Icons expected = objectFactory.createIcons();
        Icon iconA = objectFactory.createIcon();
        iconA.setName("iconA");
        iconA.setIconSetName("testTheme");
        iconA.setIconSetType("TYPETEST");
        iconA.setCssClass("testclass");
        expected.getIcons().add(iconA);
        Icon iconB = objectFactory.createIcon();
        iconB.setName("iconB");
        iconB.setIconSetName("testTheme");
        iconB.setIconSetType("TYPETEST2");
        iconB.setUrl("http://test/url");
        expected.getIcons().add(iconB);
        expected.getMissingIcons().add("unknownIcon");
        assertEquals(marshal(expected), marshal(response));
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
        verify(this.iconSetManager, never()).getDefaultIconSet();
    }

    @Test
    void getIconsIconManagerException() throws Exception
    {
        IconException iconException = new IconException("icon error");

        when(this.iconManager.hasIcon(any(), any())).thenThrow(iconException);
        when(this.iconSetManager.getCurrentIconSet()).thenReturn(new IconSet("testTheme"));

        List<String> names = asList("iconA", "unknownIcon", "iconB");
        WebApplicationException webApplicationException =
            assertThrows(WebApplicationException.class, () -> this.iconThemesResource.getIcons("wikiTest", names));

        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), webApplicationException.getResponse().getStatus());
        assertEquals(iconException, webApplicationException.getCause());

        verify(this.modelContext).setCurrentEntityReference(new WikiReference("wikiTest"));
        verify(this.modelContext).setCurrentEntityReference(CURRENT_ENTITY);
        verify(this.iconSetManager, never()).getIconSet(any());
        verify(this.iconSetManager, never()).getDefaultIconSet();
    }

    @Test
    void getIconsWikiDoesNotExist() throws Exception
    {
        when(this.wikiDescriptorManager.exists(any(String.class))).thenReturn(false);
        WebApplicationException webApplicationException =
            assertThrows(WebApplicationException.class, () -> this.iconThemesResource.getIcons("unknown", null));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
    }

    @Test
    void getIconsWikiManagerFail() throws Exception
    {
        when(this.wikiDescriptorManager.exists(any(String.class))).thenThrow(WikiManagerException.class);
        WebApplicationException webApplicationException =
            assertThrows(WebApplicationException.class, () -> this.iconThemesResource.getIcons("unknown", null));
        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), webApplicationException.getResponse().getStatus());
        assertEquals(WikiManagerException.class, webApplicationException.getCause().getClass());
    }

    private String marshal(Icons icons) throws JAXBException
    {
        // We need to marshal because jaxb generated objects does not have equalily operations.
        JAXBContext jaxbContext = JAXBContext.newInstance(Icons.class);
        StringWriter writer = new StringWriter();
        jaxbContext.createMarshaller().marshal(icons, writer);
        return writer.toString();
    }
}
