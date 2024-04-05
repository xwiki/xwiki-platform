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
package org.xwiki.netflux.script;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link NetfluxScriptService}.
 */
@ComponentTest
class NetfluxScriptServiceTest
{
    @InjectMockComponents
    private NetfluxScriptService netfluxScriptService;

    @MockComponent
    private EntityChannelStore channelStore;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Test
    void getChannel()
    {
        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");
        List<String> path = List.of("en", "content", "wysiwyg");
        EntityChannel entityChannel = new EntityChannel(documentReference, path, "qwerty");
        when(this.channelStore.getChannel(documentReference, path)).thenReturn(Optional.of(entityChannel));

        assertNull(this.netfluxScriptService.getChannel(documentReference, path));
        verify(this.channelStore, never()).getChannel(documentReference, path);

        when(this.authorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        assertSame(entityChannel, this.netfluxScriptService.getChannel(documentReference, path));
    }
}
