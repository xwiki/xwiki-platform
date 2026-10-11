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
package com.xpn.xwiki.objects;

import java.util.List;

import org.dom4j.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.internal.XWikiLegacyPasswordEncoder;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BaseCollection} class.
 *
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class BaseCollectionTest
{
    private static final String FIELD = "myField";

    private static final String PASSWORD = "secret";

    /**
     * The legacy hash of {@link #PASSWORD}, using SHA-512 and the salt {@code abcd}.
     */
    private static final String LEGACY_PASSWORD_HASH = "hash:SHA-512:abcd:b352183394a5006c92614d401e22ef6ace71a3c46b3"
        + "ef4109508fb59711282cd011d7735da75dc6672ba8e413887d30a89e33aa30e2fdec1a562128425408981";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getXClassWithNullReference()
    {
        BaseCollection collection = new BaseCollection()
        {
            @Override
            public Element toXML(BaseClass bclass)
            {
                return null;
            }
        };

        assertNull(collection.getXClass(new XWikiContext()));
    }

    @Test
    void getDiffWhenNewPropertyIsNull()
    {
        PropertyClass propertyClass = mock(PropertyClass.class);
        when(propertyClass.getClassType()).thenReturn("StringClass");
        BaseClass xclass = mock(BaseClass.class);
        when(xclass.getField(FIELD)).thenReturn(propertyClass);

        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldProperty.getValue()).thenReturn("oldValue");
        when(oldProperty.toText()).thenReturn("oldValue");

        // addField() stores the property as given, so a null one leaves the field name in the collection
        // without a value.
        BaseCollection<EntityReference> newCollection = createCollection(xclass);
        newCollection.addField(FIELD, null);
        BaseCollection<EntityReference> oldCollection = createCollection(xclass);
        oldCollection.addField(FIELD, oldProperty);

        ObjectDiff diff = getChangedPropertyDiff(newCollection.getDiff(oldCollection, this.oldcore.getXWikiContext()));

        assertEquals(FIELD, diff.getPropName());
        assertEquals("StringClass", diff.getPropType());
        assertEquals("oldValue", diff.getPrevValue());
        assertEquals("", diff.getNewValue());
    }

    @Test
    void getDiffWhenNewPropertyIsNullAndNoPropertyClass()
    {
        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldProperty.toText()).thenReturn("oldValue");

        // Without a property class the diff falls back on the plain values, where a missing property is empty too.
        BaseCollection<EntityReference> newCollection = createCollection(null);
        newCollection.addField(FIELD, null);
        BaseCollection<EntityReference> oldCollection = createCollection(null);
        oldCollection.addField(FIELD, oldProperty);

        ObjectDiff diff = getChangedPropertyDiff(newCollection.getDiff(oldCollection, this.oldcore.getXWikiContext()));

        assertEquals(FIELD, diff.getPropName());
        assertEquals("", diff.getPropType());
        assertEquals("oldValue", diff.getPrevValue());
        assertEquals("", diff.getNewValue());
    }

    @Test
    void isPasswordValueMatchingWhenClearPasswordStoredInStringProperty()
    {
        // A password stored in clear in a StringProperty is the legacy case: the property type predates
        // PasswordProperty and the value is not hashed, so the comparison is done on the raw values.
        BaseCollection<EntityReference> collection = createCollection(null);
        collection.setStringValue(FIELD, "secret");

        assertTrue(collection.isPasswordValueMatching(FIELD, "secret"));
        // Passwords are case sensitive: PasswordClass#arePasswordsMatching compares clear passwords with Strings.CS
        // and the same must hold here.
        assertFalse(collection.isPasswordValueMatching(FIELD, "SECRET"));
    }

    @Test
    void isPasswordValueMatchingWhenLegacyHashStoredInStringProperty()
    {
        // Passwords of old user profiles can be stored in a StringProperty instead of a PasswordProperty: they are not
        // re-encoded by XWikiLegacyPasswordEncoder, so they still rely on the legacy algorithm they were hashed with.
        BaseObject userObject = createUserObject();
        userObject.setStringValue(FIELD, LEGACY_PASSWORD_HASH);

        assertTrue(userObject.isPasswordValueMatching(FIELD, PASSWORD));

        // The log must name the legacy algorithm the password relies on, not the one used for re-encoded passwords.
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("An outdated algorithm [SHA-512] (or an outdated version of it) is used in a PasswordClass "
            + "property not yet attached to an object", this.logCapture.getMessage(0));
    }

    @Test
    void isPasswordValueMatchingWhenReencodedLegacyHashStoredInStringProperty()
    {
        BaseObject userObject = createUserObject();
        userObject.setStringValue(FIELD, new XWikiLegacyPasswordEncoder().reencodePassword(LEGACY_PASSWORD_HASH));

        assertTrue(userObject.isPasswordValueMatching(FIELD, PASSWORD));
        assertFalse(userObject.isPasswordValueMatching(FIELD, "SECRET"));

        // Once re-encoded by XWikiLegacyPasswordEncoder the password is protected by argon2: nothing to warn about.
        assertEquals(0, this.logCapture.size());
    }

    private BaseObject createUserObject()
    {
        BaseObject userObject = new BaseObject();
        userObject.setDocumentReference(new DocumentReference("xwiki", "XWiki", "Admin"));
        userObject.setXClassReference(new LocalDocumentReference("XWiki", "XWikiUsers"));
        return userObject;
    }

    /**
     * @param xclass the class to return from {@link BaseCollection#getXClass(XWikiContext)}, so that the test needs
     *            no wiki to resolve it
     * @return a collection using {@link BaseCollection}'s own {@code getDiff()}, which both {@link BaseObject} and
     *         {@link BaseClass} override
     */
    private BaseCollection<EntityReference> createCollection(BaseClass xclass)
    {
        return new BaseCollection<>()
        {
            @Override
            public Element toXML(BaseClass bclass)
            {
                return null;
            }

            @Override
            public BaseClass getXClass(XWikiContext context)
            {
                return xclass;
            }
        };
    }

    private ObjectDiff getChangedPropertyDiff(List<ObjectDiff> diffs)
    {
        return diffs.stream().filter(diff -> ObjectDiff.ACTION_PROPERTYCHANGED.equals(diff.getAction())).findFirst()
            .orElseThrow();
    }
}
