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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StringClass;
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

    @Mock
    private XWikiContext xcontext;
    
    @InjectComponentManager
    private ComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @MockComponent
    private SpaceReferenceResolver<String> resolver;

    @InjectMockComponents
    private ModelFactory modelFactory;

    private URI dummyUrl;

    private Document testDocument;

    @BeforeEach
    public void mockUpTestDocument() throws Exception
    {
        dummyUrl = new URI("https://localhost/");
        testDocument = mock(Document.class);
        SpaceReference spaceRef = new SpaceReference("Space", new WikiReference("wiki"));
        when(testDocument.getPrefixedFullName()).thenReturn("wiki:Space.Page");
        when(testDocument.getWiki()).thenReturn("wiki");
        when(testDocument.getSpace()).thenReturn("Space");
        when(testDocument.getName()).thenReturn("Page");
        when(testDocument.getDocumentReference()).thenReturn(new DocumentReference("Page", spaceRef));

        when(xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWikiId()).thenReturn("wiki");
    }

    /**
     * a separate set up only for tests which needs an object
     * @return the object for test, prefilled witha few values
     */
    private BaseObject setUpTestObject() throws Exception
    {
        BaseObject xwikiObject = mock(BaseObject.class);
        BaseClass xwikiClass  = mock(BaseClass.class);
       
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

        Object result = modelFactory.toRestObject(dummyUrl, testDocument, xwikiObject, false, false);

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

        Object result = modelFactory.toRestObject(dummyUrl, testDocument, xwikiObject, false, false);

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
}
