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
package org.xwiki.mail.internal.factory.template;

import java.util.Collections;

import javax.mail.MessagingException;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.SecureMailTemplateManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class SecureMailTemplateManagerTest
{
    @Rule
    public MockitoComponentMockingRule<SecureMailTemplateManager> mocker =
        new MockitoComponentMockingRule<>(SecureMailTemplateManager.class);

    @Test
    public void evaluateWhenNotAuthorized() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getCurrentUserReference()).thenReturn(new DocumentReference("userwiki", "userspace", "userpage"));

        try {
            this.mocker.getComponentUnderTest().evaluate(reference, "property", Collections.<String, String>emptyMap());
            fail("Should have thrown an exception");
        } catch (MessagingException expected) {
            assertEquals("Current user [userwiki:userspace.userpage] has no permission to view Mail Template "
                + "Document [wiki:space.page]", expected.getMessage());
        }
    }
}
