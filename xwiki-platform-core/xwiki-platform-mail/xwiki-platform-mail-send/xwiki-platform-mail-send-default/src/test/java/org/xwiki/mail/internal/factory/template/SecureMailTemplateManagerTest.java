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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.SecureMailTemplateManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentTest
public class SecureMailTemplateManagerTest
{
    @InjectMockComponents
    private SecureMailTemplateManager mailTemplateManager;

    @MockComponent
    private DocumentAccessBridge dab;

    @Test
    void evaluateWhenNotAuthorized()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(this.dab.getCurrentUserReference()).thenReturn(new DocumentReference("userwiki", "userspace", "userpage"));

        Throwable exception = assertThrows(MessagingException.class, () -> {
            this.mailTemplateManager.evaluate(reference, "property", Collections.emptyMap());
        });
        assertEquals("Current user [userwiki:userspace.userpage] has no permission to view Mail Template "
            + "Document [wiki:space.page]", exception.getMessage());
    }
}
