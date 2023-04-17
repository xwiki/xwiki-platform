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
package org.xwiki.rest.internal.resources.objects;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.doc.DefaultDocumentRevisionProvider;
import com.xpn.xwiki.internal.mandatory.TagClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ObjectsAtPageVersionResourceImpl}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({
    DefaultDocumentRevisionProvider.class,
    TagClassDocumentInitializer.class,
})
@ReferenceComponentList
class ObjectsAtPageVersionResourceImplTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "space", "page");

    private static final DocumentReference TAG_CLASS = new DocumentReference("xwiki", "XWiki", "TagClass");

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final String TAG_VALUE = "tagValue";

    @MockComponent
    @Named("database")
    private DocumentRevisionProvider databaseDocumentRevisionProvider;

    @MockComponent
    @Named("deleted")
    private DocumentRevisionProvider deletedDocumentRevisionProvider;

    @InjectMockComponents
    private ObjectsAtPageVersionResourceImpl objectsAtPageVersionResource;

    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @Mock
    private UriInfo uriInfo;

    @MockComponent
    private ModelFactory modelFactory;

    private XWikiDocument document;

    @BeforeEach
    public void setUp() throws XWikiException, URISyntaxException, IllegalAccessException
    {
        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));
        FieldUtils.writeField(this.objectsAtPageVersionResource, "uriInfo", this.uriInfo, true);

        this.document = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject baseObject = this.document.newXObject(TAG_CLASS, this.mockitoOldcore.getXWikiContext());
        baseObject.setStringListValue(XWikiConstant.TAG_CLASS_PROP_TAGS, List.of(TAG_VALUE));
        this.mockitoOldcore.getSpyXWiki().saveDocument(this.document, this.mockitoOldcore.getXWikiContext());
        this.mockitoOldcore.getXWikiContext().setUserReference(USER);
    }

    @Test
    void getDeletedObjects() throws XWikiException, XWikiRestException, AuthorizationException
    {
        String version = "deleted:1";
        XWikiDocument deletedVersion = this.document.clone();
        BaseObject baseObject = deletedVersion.getXObject(TAG_CLASS);
        baseObject.setStringListValue(XWikiConstant.TAG_CLASS_PROP_TAGS, List.of("deleted"));
        when(this.deletedDocumentRevisionProvider.getRevision(this.document.getDocumentReferenceWithLocale(),
            "1")).thenReturn(deletedVersion);
        when(this.mockitoOldcore.getMockRightService().hasAccessLevel("view",
            this.mockitoOldcore.getXWikiContext().getUser(), this.document.getPrefixedFullName(),
            this.mockitoOldcore.getXWikiContext())).thenReturn(true);

        Objects objects = this.objectsAtPageVersionResource.getObjects(DOCUMENT_REFERENCE.getWikiReference().getName(),
            DOCUMENT_REFERENCE.getParent().getName(), DOCUMENT_REFERENCE.getName(), version, 0, 10, false);
        List<ObjectSummary> objectSummaries = objects.getObjectSummaries();
        assertEquals(1, objectSummaries.size());

        verify(this.modelFactory).toRestObjectSummary(this.uriInfo.getBaseUri(), new Document(deletedVersion,
            this.mockitoOldcore.getXWikiContext()), baseObject, true, false);

        doThrow(new AuthorizationException("Access denied")).when(this.deletedDocumentRevisionProvider)
            .checkAccess(Right.VIEW, CurrentUserReference.INSTANCE, DOCUMENT_REFERENCE, "1");

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.objectsAtPageVersionResource.getObjects(DOCUMENT_REFERENCE.getWikiReference().getName(),
                DOCUMENT_REFERENCE.getParent().getName(), DOCUMENT_REFERENCE.getName(), version, 0, 10, false));
        assertEquals(404, webApplicationException.getResponse().getStatus());
    }
}
