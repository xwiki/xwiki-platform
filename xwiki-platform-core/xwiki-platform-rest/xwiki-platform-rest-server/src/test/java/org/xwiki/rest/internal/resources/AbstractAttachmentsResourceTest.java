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
package org.xwiki.rest.internal.resources;

import java.net.URI;

import javax.inject.Named;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public abstract class AbstractAttachmentsResourceTest
{
    @MockComponent
    protected ModelFactory modelFactory;

    @MockComponent
    protected QueryManager queryManager;

    @MockComponent
    protected SpaceReferenceResolver<String> defaultSpaceReferenceResover;

    @MockComponent
    @Named("local")
    protected EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @InjectMockitoOldcore
    protected MockitoOldcore oldCore;

    protected XWikiContext xcontext;

    protected XWiki xwiki;

    @Mock
    protected UriInfo uriInfo;

    @BeforeEach
    public void setUp() throws Exception
    {
        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));

        this.xcontext = this.oldCore.getXWikiContext();
        this.xwiki = this.oldCore.getSpyXWiki();
    }

    protected void setUriInfo(XWikiResource resource) throws Exception
    {
        FieldUtils.writeField(resource, "uriInfo", this.uriInfo, true);
    }

    protected void mockPreifxQueryParam(Query query, String parameterName, String value)
    {
        QueryParameter parameter = mock(QueryParameter.class, "parameter");
        when(query.bindValue(parameterName)).thenReturn(parameter);

        QueryParameter prefixLiteral = mock(QueryParameter.class, "prefixLiteral");
        when(parameter.literal(value)).thenReturn(prefixLiteral);

        QueryParameter endsWithAnyChars = mock(QueryParameter.class, "endWithAnyChars");
        when(prefixLiteral.anyChars()).thenReturn(endsWithAnyChars);

        when(endsWithAnyChars.query()).thenReturn(query);
    }

    protected void mockSuffixQueryParam(Query query, String parameterName, String value)
    {
        QueryParameter parameter = mock(QueryParameter.class, "parameter");
        when(query.bindValue(parameterName)).thenReturn(parameter);

        QueryParameter startsWithAnyChars = mock(QueryParameter.class, "startsWithAnyChars");
        when(parameter.anyChars()).thenReturn(startsWithAnyChars);

        QueryParameter suffixLiteral = mock(QueryParameter.class, "suffixLiteral");
        when(startsWithAnyChars.literal(value)).thenReturn(suffixLiteral);

        when(suffixLiteral.query()).thenReturn(query);
    }

    protected void mockContainsQueryParam(Query query, String parameterName, String value)
    {
        QueryParameter parameter = mock(QueryParameter.class, "parameter");
        when(query.bindValue(parameterName)).thenReturn(parameter);

        QueryParameter startWithAnyChars = mock(QueryParameter.class, "startWithAnyChars");
        when(parameter.anyChars()).thenReturn(startWithAnyChars);

        QueryParameter containsLiteral = mock(QueryParameter.class, "containsLiteral");
        when(startWithAnyChars.literal(value)).thenReturn(containsLiteral);

        QueryParameter endsWithAnyChars = mock(QueryParameter.class, "endWithAnyChars");
        when(containsLiteral.anyChars()).thenReturn(endsWithAnyChars);

        when(endsWithAnyChars.query()).thenReturn(query);
    }
}
