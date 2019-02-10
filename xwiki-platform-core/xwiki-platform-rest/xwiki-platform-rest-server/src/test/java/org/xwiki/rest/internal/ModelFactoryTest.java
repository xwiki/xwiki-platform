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

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.junit5.mockito.ComponentTest;
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import javax.inject.Provider;

@ComponentTest
public class ModelFactoryTest
{
    protected XWikiContext xcontext = mock(XWikiContext.class);
    
    @InjectComponentManager
    protected ComponentManager componentManager;

    @MockComponent
    protected Provider<XWikiContext> xcontextProvider;

    @MockComponent
    SpaceReferenceResolver<String> resolver;
    
    @InjectMockComponents
    ModelFactory modelFactory;
    
    @Test
    @SuppressWarnings("deprecation")
    void testToRestObject() throws Exception
    {
        when(xcontextProvider.get()).thenReturn(xcontext);
        
        BaseObject xwikiObject = mock(BaseObject.class);
        BaseClass xwikiClass  = mock(BaseClass.class);
       
        when(xwikiObject.getPropertyNames()).thenReturn(new String[] {});
        when(xwikiObject.getXClass(xcontext)).thenReturn(xwikiClass);
        when(xwikiObject.getClassName()).thenReturn("Some.XClass");
        when(xwikiObject.getNumber()).thenReturn(0);
        
        StringClass stringField = new StringClass();
        stringField.setName("textValue");
        StringProperty textElement = new StringProperty();
        textElement.setName("textValue");
        textElement.setClassType("String");
        textElement.setValue("abcd");
        when(xwikiObject.get("textValue")).thenReturn(textElement);

        PasswordClass pwField = new PasswordClass();
        pwField.setName("passwordValue");
        StringProperty pwElement = new StringProperty();
        pwElement.setName("passwordValue");
        pwElement.setClassType("Password");
        pwElement.setValue("abcd");
        when(xwikiObject.get("passwordValue")).thenReturn(pwElement);
        
        when(xwikiClass.getProperties()).thenReturn(new java.lang.Object[] {stringField, pwField});
        
        // some more mockery needed to pass the "toRestObject" call
        // although the results are not tested (yet)

        URI dummy = new URI("https://localhost/");
        Document doc = mock(Document.class);
        SpaceReference spaceRef = new SpaceReference("Space", new WikiReference("wiki"));
        when(doc.getPrefixedFullName()).thenReturn("wiki:Space.Page");
        when(doc.getWiki()).thenReturn("wiki");
        when(doc.getSpace()).thenReturn("Space");
        when(doc.getName()).thenReturn("Page");
        when(doc.getDocumentReference()).thenReturn(new DocumentReference("Page", spaceRef));
        when(resolver.resolve(eq("Space"), any(String.class))).thenReturn(spaceRef);
        
        com.xpn.xwiki.web.Utils.setComponentManager(componentManager);
        
        //
        // now we get around to call the method under test
        //
        
        Object result = modelFactory.toRestObject(dummy, doc, xwikiObject, false, false);

        List<Property> properties = result.getProperties();
        // note: we do not mind if there is no "Password" property at all in the list
        // that is as good as not showing the value
        boolean stringFieldFound = false;
        for (Property prop : properties) {
            if ("String".equals(prop.getType())) {
                assertEquals("abcd", prop.getValue(), "should see proper value for StringField");
                stringFieldFound = true;
            }
            if ("Password".equals(prop.getType())) {
                assertNull(prop.getValue(), "should see not value for PasswordField");
            }
        }
        assertTrue(stringFieldFound, "should have StringField in the result");
    }
}
