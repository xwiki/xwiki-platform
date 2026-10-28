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

import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.TagClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ObjectPropertyResourceImpl}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList(TagClassDocumentInitializer.class)
@ReferenceComponentList
class ObjectPropertyResourceImplTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "space", "page");

    private static final DocumentReference TAG_CLASS = new DocumentReference("xwiki", "XWiki", "TagClass");

    private static final String TAG_CLASS_NAME = "XWiki.TagClass";

    @InjectMockComponents
    private ObjectPropertyResourceImpl objectPropertyResource;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ModelFactory modelFactory;

    @Mock
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.uriInfo.getBaseUri()).thenReturn(new URI("https://test/"));
        FieldUtils.writeField(this.objectPropertyResource, "uriInfo", this.uriInfo, true);

        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(Right.class), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(Right.class), any(), any())).thenReturn(true);
        // Document#hasAccessLevel, used by the resources, goes through the right service.
        when(this.oldcore.getMockRightService().hasAccessLevel(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);

        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject tagObject = document.newXObject(TAG_CLASS, xcontext);
        // A string property, so that the string value sent through REST can be set without parsing.
        tagObject.setStringValue(XWikiConstant.TAG_CLASS_PROP_TAGS, "tag");
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);

        Property tagsProperty = new Property();
        tagsProperty.setName(XWikiConstant.TAG_CLASS_PROP_TAGS);
        Object restObject = new Object();
        restObject.getProperties().add(tagsProperty);
        when(this.modelFactory.toRestObject(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(restObject);
    }

    @Test
    void updateObjectPropertyWithCommentAndMinorRevision() throws Exception
    {
        this.objectPropertyResource.updateObjectProperty("xwiki", "space", "page", TAG_CLASS_NAME, 0,
            XWikiConstant.TAG_CLASS_PROP_TAGS, true, "Changed the tags", newTagsProperty());

        XWikiDocument savedDocument = getSavedDocument();
        assertEquals("newTag",
            savedDocument.getXObject(TAG_CLASS).getStringValue(XWikiConstant.TAG_CLASS_PROP_TAGS));
        assertEquals("Changed the tags", savedDocument.getComment());
        assertTrue(savedDocument.isMinorEdit());
    }

    @Test
    void updateObjectPropertyWithoutComment() throws Exception
    {
        this.objectPropertyResource.updateObjectProperty("xwiki", "space", "page", TAG_CLASS_NAME, 0,
            XWikiConstant.TAG_CLASS_PROP_TAGS, null, null, newTagsProperty());

        XWikiDocument savedDocument = getSavedDocument();
        assertEquals("", savedDocument.getComment());
        assertFalse(savedDocument.isMinorEdit());
    }

    private Property newTagsProperty()
    {
        Property property = new Property();
        property.setValue("newTag");
        return property;
    }

    private XWikiDocument getSavedDocument() throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
    }
}
