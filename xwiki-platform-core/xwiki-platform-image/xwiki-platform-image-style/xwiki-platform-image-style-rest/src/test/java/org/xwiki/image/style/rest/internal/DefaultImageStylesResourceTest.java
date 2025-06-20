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
package org.xwiki.image.style.rest.internal;

import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.image.style.ImageStyleConfiguration;
import org.xwiki.image.style.ImageStyleManager;
import org.xwiki.image.style.model.ImageStyle;
import org.xwiki.image.style.rest.model.jaxb.Style;
import org.xwiki.image.style.rest.model.jaxb.Styles;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiResponse;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultImageStylesResource}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class DefaultImageStylesResourceTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @InjectMockComponents
    private DefaultImageStylesResource imageStylesResource;

    @MockComponent
    private ImageStyleConfiguration imageStyleConfiguration;

    @MockComponent
    private ImageStyleManager imageStyleManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiResponse response;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getResponse()).thenReturn(this.response);
    }

    @Test
    void getStyles() throws Exception
    {
        ImageStyle imageStyle = new ImageStyle();
        imageStyle.setIdentifier("testImageStyle");
        when(this.imageStyleManager.getImageStyles("wiki")).thenReturn(Set.of(imageStyle));
        Styles wiki = this.imageStylesResource.getStyles("wiki");
        Styles expectedStyles = new Styles();
        Style expectedStyle = new Style();
        expectedStyle.setIdentifier("testImageStyle");
        expectedStyles.getImageStyles().add(expectedStyle);
        assertEquals(toJson(expectedStyles), toJson(wiki));
    }

    @Test
    void getDefaultStyleIdentifier() throws Exception
    {
        when(this.imageStyleConfiguration.getDefaultStyle("wiki", "xwiki:Space.Page")).thenReturn("defaultStyle");
        assertEquals(Map.of(
            "defaultStyle", "defaultStyle",
            "forceDefaultStyle", "false"
        ), this.imageStylesResource.getDefaultStyleIdentifier("wiki", "xwiki:Space.Page").getEntity());
    }

    @Test
    void getDefaultStyleIdentifierNotFound() throws Exception
    {
        when(this.imageStyleConfiguration.getDefaultStyle("wiki", "xwiki:Space.Page")).thenReturn("");
        Response response = this.imageStylesResource.getDefaultStyleIdentifier("wiki", "xwiki:Space.Page");
        assertEquals(Map.of(), response.getEntity());
        assertEquals(OK.getStatusCode(), response.getStatus());
   }

    private String toJson(Styles wiki) throws JsonProcessingException
    {
        return OBJECT_MAPPER.writeValueAsString(wiki);
    }
}
