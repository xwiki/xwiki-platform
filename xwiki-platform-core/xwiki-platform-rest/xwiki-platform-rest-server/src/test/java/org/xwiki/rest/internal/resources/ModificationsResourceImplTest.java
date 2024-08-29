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
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.web.Utils;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Test of {@link ModificationsResourceImpl}.
 *
 * @version $Id$
 * @since 14.6
 * @since 14.4.3
 * @since 13.10.8
 */
@ComponentTest
class ModificationsResourceImplTest
{
    @InjectMockComponents
    private ModificationsResourceImpl modificationsResource;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Mock
    private Query query;

    @Mock
    private XWikiContext xcontext;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
    }

    @Test
    void getModifications() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("/xwiki/rest"));
        ReflectionUtils.setFieldValue(this.modificationsResource, "uriInfo", uriInfo);
        Utils.setComponentManager(this.componentManager);

        when(this.queryManager.createQuery(
            "select doc.space, doc.name, doc.language, rcs.id, rcs.date, rcs.author, rcs.comment "
                + "from XWikiRCSNodeInfo as rcs, XWikiDocument as doc "
                + "where rcs.id.docId = doc.id "
                + "and rcs.date > :date "
                + "order by rcs.date desc, rcs.id.version1 desc, rcs.id.version2 desc",
            Query.XWQL)).thenReturn(this.query);
        when(this.query.bindValue(any(), any())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.setWiki(any())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(asList(
            new Object[] {
                "Space",
                "Viewable",
                "en",
                new XWikiRCSNodeId(new WikiReference("wikiId"), 42, new Version(1, 1)),
                new Timestamp(1),
                "modifierViewable",
                "commentViewable"
            },
            new Object[] {
                "Space",
                "NotViewable",
                "fr",
                new XWikiRCSNodeId(new WikiReference("wikiId"), 43, new Version(1, 2)),
                new Timestamp(2),
                "modifierNotViewable",
                "commentNotViewable"
            }
        ));

        DocumentReference viewableDocumentReference = new DocumentReference("wikiId", "Space", "Viewable");
        DocumentReference notViewableDocumentReference = new DocumentReference("wikiId", "Space", "NotViewable");
        SpaceReference whateverSpaceReference = new SpaceReference("Space", new WikiReference("whatever"));
        String viewableSerializedReference = "wikiId:Space.Viewable";
        String notViewableSerializedReference = "wikiId:Space.NotViewable";

        when(this.authorizationManager.hasAccess(VIEW, viewableDocumentReference)).thenReturn(true);
        when(this.authorizationManager.hasAccess(VIEW, notViewableDocumentReference)).thenReturn(false);
        when(this.spaceReferenceResolver.resolve("Space", new WikiReference("whatever")))
            .thenReturn(whateverSpaceReference);
        when(this.entityReferenceSerializer.serialize(viewableDocumentReference))
            .thenReturn(viewableSerializedReference);
        when(this.resolver.resolve(viewableSerializedReference)).thenReturn(viewableDocumentReference);
        when(this.entityReferenceSerializer.serialize(notViewableDocumentReference))
            .thenReturn(notViewableSerializedReference);
        when(this.resolver.resolve(notViewableSerializedReference)).thenReturn(notViewableDocumentReference);
        when(this.localEntityReferenceSerializer.serialize(whateverSpaceReference)).thenReturn("Space");

        History history = this.modificationsResource.getModifications("wikiId", 0, 10, "desc", 1L, false);

        HistorySummary expectedHistorySummary = new HistorySummary();
        expectedHistorySummary.setPageId("wikiId:Space.Viewable");
        expectedHistorySummary.setWiki("wikiId");
        expectedHistorySummary.setSpace("Space");
        expectedHistorySummary.setName("Viewable");
        expectedHistorySummary.setVersion("1.1");
        expectedHistorySummary.setMajorVersion(1);
        expectedHistorySummary.setMinorVersion(1);
        Calendar expectedCalendar = Calendar.getInstance();
        expectedCalendar.setTime(new Date(new Timestamp(1).getTime()));
        expectedHistorySummary.setModified(expectedCalendar);
        expectedHistorySummary.setModifier("modifierViewable");
        expectedHistorySummary.setLanguage("en");
        expectedHistorySummary.setComment("commentViewable");
        Link expectedLink = new Link();
        expectedLink.setHref("/xwiki/rest/wikis/wikiId/spaces/Space/pages/Viewable/translations/en/history/1.1");
        expectedLink.setRel("http://www.xwiki.org/rel/page");
        expectedHistorySummary.withLinks(expectedLink);
        ObjectMapper objectMapper = new ObjectMapper();
        assertEquals(objectMapper.writeValueAsString(singleton(expectedHistorySummary)),
            objectMapper.writeValueAsString(history.getHistorySummaries()));
    }
}
