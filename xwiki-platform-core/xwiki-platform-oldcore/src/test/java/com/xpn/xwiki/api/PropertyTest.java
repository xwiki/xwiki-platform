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
package com.xpn.xwiki.api;

import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Property}.
 *
 * @version $Id$
 */
@OldcoreTest
class PropertyTest
{
    private static final DocumentReference CLASS_REFERENCE = new DocumentReference("wiki", "Some", "Class");

    private static final String TEXT_FIELD = "text";

    private static final String PASSWORD_FIELD = "password";

    private static final String EMAIL_FIELD = "email";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    @MockComponent
    private EmailAddressObfuscator emailAddressObfuscator;

    private Object object;

    @BeforeEach
    void setUp()
    {
        BaseClass xclass = new BaseClass();
        xclass.setDocumentReference(CLASS_REFERENCE);
        xclass.addTextField(TEXT_FIELD, "Text", 30);
        xclass.addPasswordField(PASSWORD_FIELD, "Password", 30);
        xclass.addEmailField(EMAIL_FIELD, "Email", 30);

        BaseObject xobject = new BaseObject();
        xobject.setXClassReference(CLASS_REFERENCE);
        xobject.setSourceXClass(xclass);
        xobject.setStringValue(TEXT_FIELD, "some text");
        xobject.setStringValue(PASSWORD_FIELD, "s3cr3t");
        xobject.setStringValue(EMAIL_FIELD, "alice@example.com");

        this.object = new Object(xobject, this.oldcore.getXWikiContext());
    }

    @Test
    void getValueOfNonSensitivePropertyDoesNotCheckProgrammingRight()
    {
        assertEquals("some text", this.object.getProperty(TEXT_FIELD).getValue());

        verifyNoInteractions(this.oldcore.getMockRightService());
    }

    @Test
    void getValueOfSensitivePropertyWithoutProgrammingRight()
    {
        // PasswordClass doesn't override PropertyClass#getObfuscatedValue(Object), which returns null for safety,
        // and BaseStringProperty turns that null into an empty string.
        assertEquals("", this.object.getProperty(PASSWORD_FIELD).getValue());
    }

    @Test
    void getValueOfSensitivePropertyWithProgrammingRight()
    {
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(true);

        assertEquals("s3cr3t", this.object.getProperty(PASSWORD_FIELD).getValue());
    }

    @Test
    void getValueOfEmailPropertyWhenObfuscationIsDisabled()
    {
        when(this.mailConfiguration.shouldObfuscate()).thenReturn(false);

        assertEquals("alice@example.com", this.object.getProperty(EMAIL_FIELD).getValue());

        // An email address is only sensitive when the mail configuration asks for obfuscation, so the programming
        // right is not relevant here either.
        verifyNoInteractions(this.oldcore.getMockRightService());
        verifyNoInteractions(this.emailAddressObfuscator);
    }

    @Test
    void getValueOfEmailPropertyWhenObfuscationIsEnabled()
    {
        when(this.mailConfiguration.shouldObfuscate()).thenReturn(true);
        when(this.emailAddressObfuscator.obfuscate(any(InternetAddress.class))).thenReturn("a...e@example.com");

        assertEquals("a...e@example.com", this.object.getProperty(EMAIL_FIELD).getValue());
    }
}
