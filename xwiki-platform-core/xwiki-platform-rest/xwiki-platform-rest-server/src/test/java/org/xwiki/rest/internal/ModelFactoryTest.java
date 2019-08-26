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
package org.xwiki.rest.internal;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Hierarchy;
import org.xwiki.rest.model.jaxb.HierarchyItem;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@OldcoreTest
public class ModelFactoryTest
{
    private static final String TEST_STRING_FIELD = "textValue";

    private static final String TEST_STRING_VALUE = "abcd";

    private static final String TEST_PASSWORD_FIELD = "passwordValue";

    private static final String TEST_PASSWORD_VALUE = "secret";

    @InjectComponentManager
    private ComponentManager componentManager;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private SpaceReferenceResolver<String> resolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @InjectMockComponents
    private ModelFactory modelFactory;

    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    private URI baseURI;

    private Document testDocument;

    @BeforeEach
    public void mockUpTestDocument() throws Exception
    {
        baseURI = new URI("https://localhost/");
        testDocument = mock(Document.class);
        WikiReference wikiRef = new WikiReference("wiki");
        SpaceReference spaceRef = new SpaceReference("Space", wikiRef);
        when(testDocument.getPrefixedFullName()).thenReturn("wiki:Space.Page");
        when(testDocument.getWiki()).thenReturn("wiki");
        when(testDocument.getSpace()).thenReturn("Space");
        when(testDocument.getName()).thenReturn("Page");
        when(testDocument.getDocumentReference()).thenReturn(new DocumentReference("Page", spaceRef));

        this.xcontext = this.oldCore.getXWikiContext();
        this.xcontext.setWikiReference(wikiRef);
        this.xcontext.setWiki(this.xwiki);
    }

    /**
     * A separate set up only for tests which needs an object.
     * 
     * @return the object for test, prefilled witha few values
     */
    private BaseObject setUpTestObject() throws Exception
    {
        BaseObject xwikiObject = mock(BaseObject.class);
        BaseClass xwikiClass = mock(BaseClass.class);

        when(xwikiObject.getPropertyNames()).thenReturn(new String[] {});
        when(xwikiObject.getXClass(xcontext)).thenReturn(xwikiClass);
        when(xwikiObject.getClassName()).thenReturn("Some.XClass");
        when(xwikiObject.getNumber()).thenReturn(0);

        StringClass stringField = new StringClass();
        stringField.setName(TEST_STRING_FIELD);
        StringProperty textElement = new StringProperty();
        textElement.setName(TEST_STRING_FIELD);
        textElement.setClassType("String");
        textElement.setValue(TEST_STRING_VALUE);
        when(xwikiObject.get(TEST_STRING_FIELD)).thenReturn(textElement);

        PasswordClass pwField = new PasswordClass();
        pwField.setName(TEST_PASSWORD_FIELD);
        StringProperty pwElement = new StringProperty();
        pwElement.setName(TEST_PASSWORD_FIELD);
        pwElement.setClassType("Password");
        pwElement.setValue(TEST_PASSWORD_VALUE);
        when(xwikiObject.get(TEST_PASSWORD_FIELD)).thenReturn(pwElement);

        when(xwikiClass.getProperties()).thenReturn(new java.lang.Object[] {stringField, pwField});

        return xwikiObject;
    }

    @Test
    void toRestObjectCheckWhichObjectValuesAreAvailableForNonAdmins() throws Exception
    {
        when(authorizationManager.hasAccess(Right.ADMIN, new WikiReference("wiki"))).thenReturn(false);

        BaseObject xwikiObject = setUpTestObject();

        Object result = modelFactory.toRestObject(baseURI, testDocument, xwikiObject, false, false);

        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put(TEST_STRING_FIELD, TEST_STRING_VALUE);
        expectedValues.put(TEST_PASSWORD_FIELD, null);
        assertExpectedPropertyValues(result.getProperties(), expectedValues);
    }

    @Test
    void toRestObjectCheckWhichObjectValuesAreAvailableForAdmins() throws Exception
    {
        when(authorizationManager.hasAccess(Right.ADMIN, new WikiReference("wiki"))).thenReturn(true);

        BaseObject xwikiObject = setUpTestObject();

        Object result = modelFactory.toRestObject(baseURI, testDocument, xwikiObject, false, false);

        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put(TEST_STRING_FIELD, TEST_STRING_VALUE);
        expectedValues.put(TEST_PASSWORD_FIELD, TEST_PASSWORD_VALUE);
        assertExpectedPropertyValues(result.getProperties(), expectedValues);
    }

    private void assertExpectedPropertyValues(List<Property> properties, Map<String, String> expectedValues)
    {
        Set<String> propertiesFound = new HashSet<>();
        for (Property prop : properties) {
            String expectedValue = expectedValues.get(prop.getName());
            assertEquals(expectedValue, prop.getValue(),
                String.format("unexpected value for property name [%s]", prop.getName()));
            propertiesFound.add(prop.getName());
        }
        Set<String> missingProperties = new HashSet<>(expectedValues.keySet());
        missingProperties.removeAll(propertiesFound);

        assertTrue(missingProperties.isEmpty(), String.format("missing properties %s in result", missingProperties));
    }

    @Test
    public void toRestHierarchyFromSpaceWithoutPrettyNames()
    {
        SpaceReference spaceReference = new SpaceReference("dev", "API");
        when(this.xwiki.getURL(spaceReference.getParent(), this.xcontext)).thenReturn("wiki URL");
        when(this.xwiki.getURL(spaceReference, this.xcontext)).thenReturn("space URL");

        Hierarchy hierarchy = this.modelFactory.toRestHierarchy(spaceReference, false);

        assertEquals(2, hierarchy.getItems().size());

        HierarchyItem item = hierarchy.getItems().get(0);
        assertEquals(item.getLabel(), item.getName());
        assertEquals("dev", item.getName());
        assertEquals("wiki", item.getType());
        assertEquals("wiki URL", item.getUrl());

        item = hierarchy.getItems().get(1);
        assertEquals(item.getLabel(), item.getName());
        assertEquals("API", item.getName());
        assertEquals("space", item.getType());
        assertEquals("space URL", item.getUrl());
    }

    @Test
    public void toRestHierarchyFromDocumentWithPrettyNames() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("dev", "API", "WebHome");
        XWikiDocument document = mock(XWikiDocument.class, "en");
        when(this.xwiki.getDocument((EntityReference) documentReference, this.xcontext)).thenReturn(document);
        when(this.xwiki.getDocument(documentReference.getParent(), this.xcontext)).thenReturn(document);

        XWikiDocument translatedDocument = mock(XWikiDocument.class, "fr");
        when(document.getTranslatedDocument(this.xcontext)).thenReturn(translatedDocument);
        when(translatedDocument.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("API Title");

        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.FRENCH);
        when(translatedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReferenceWithLocale);
        when(this.xwiki.getURL(documentReferenceWithLocale, this.xcontext)).thenReturn("tdoc URL");

        WikiDescriptor wikiDescriptor = new WikiDescriptor("dev", "dev");
        wikiDescriptor.setPrettyName("Development Wiki");
        when(this.wikiDescriptorManager.getById("dev")).thenReturn(wikiDescriptor);

        Hierarchy hierarchy = this.modelFactory.toRestHierarchy(documentReference, true);

        assertEquals(3, hierarchy.getItems().size());

        HierarchyItem item = hierarchy.getItems().get(0);
        assertEquals("Development Wiki", item.getLabel());

        item = hierarchy.getItems().get(1);
        assertEquals("API Title", item.getLabel());
        assertEquals("tdoc URL", item.getUrl());

        item = hierarchy.getItems().get(2);
        assertEquals("API Title", item.getLabel());
        assertEquals("tdoc URL", item.getUrl());
    }

    @Test
    public void toRestAttachment()
    {
        com.xpn.xwiki.api.Attachment xwikiAttachment = mock(com.xpn.xwiki.api.Attachment.class);
        when(xwikiAttachment.getLongSize()).thenReturn(123L);
        when(xwikiAttachment.getVersion()).thenReturn("2.1");
        when(xwikiAttachment.getMimeType()).thenReturn("image/png");
        when(xwikiAttachment.getAuthor()).thenReturn("XWiki.Admin");
        when(xwikiAttachment.getDate()).thenReturn(new Date(1559347200000L));

        AttachmentReference attachmentReference =
            new AttachmentReference("logo.png", new DocumentReference("test", "Space", "Page"));
        when(xwikiAttachment.getReference()).thenReturn(attachmentReference);
        when(xwikiAttachment.getFilename()).thenReturn(attachmentReference.getName());

        Document document = mock(Document.class);
        when(xwikiAttachment.getDocument()).thenReturn(document);
        when(document.getFullName()).thenReturn("Space.Page");
        when(document.getVersion()).thenReturn("5.2");

        when(this.xwiki.getURL(attachmentReference, this.xcontext)).thenReturn("attachment URL");
        when(this.xwiki.getExternalAttachmentURL("Space.Page", "logo.png", this.xcontext))
            .thenReturn("attachment external URL");

        when(this.defaultEntityReferenceSerializer.serialize(attachmentReference.getParent()))
            .thenReturn("test:Space.Page");
        when(this.defaultEntityReferenceSerializer.serialize(attachmentReference))
            .thenReturn("test:Space.Page@logo.png");

        Attachment restAttachment = this.modelFactory.toRestAttachment(this.baseURI, xwikiAttachment, false, false);

        assertEquals(xwikiAttachment.getAuthor(), restAttachment.getAuthor());
        assertEquals(xwikiAttachment.getDate(), restAttachment.getDate().getTime());
        assertEquals(4, restAttachment.getHierarchy().getItems().size());
        assertEquals("test:Space.Page@logo.png", restAttachment.getId());
        assertEquals(3, restAttachment.getLinks().size());
        assertEquals(xwikiAttachment.getLongSize(), restAttachment.getLongSize());
        assertEquals(xwikiAttachment.getMimeType(), restAttachment.getMimeType());
        assertEquals(xwikiAttachment.getFilename(), restAttachment.getName());
        assertEquals("test:Space.Page", restAttachment.getPageId());
        assertEquals(document.getVersion(), restAttachment.getPageVersion());
        assertEquals(123, restAttachment.getSize());
        assertEquals(xwikiAttachment.getVersion(), restAttachment.getVersion());
        assertEquals("attachment external URL", restAttachment.getXwikiAbsoluteUrl());
        assertEquals("attachment URL", restAttachment.getXwikiRelativeUrl());
    }
}
