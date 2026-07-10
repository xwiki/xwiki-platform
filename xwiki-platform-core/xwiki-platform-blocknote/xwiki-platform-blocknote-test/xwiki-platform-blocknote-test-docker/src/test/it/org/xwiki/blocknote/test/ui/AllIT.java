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
package org.xwiki.blocknote.test.ui;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestClassOrder;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the BlockNote integration.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@UITest
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class AllIT
{
    @Nested
    @Order(1)
    class NestedBlockNoteIT extends BlockNoteIT
    {
    }

    @Nested
    @Order(2)
    class NestedRoundTripIT extends RoundTripIT
    {
    }

    @Nested
    @Order(3)
    class NestedImageIT extends ImageIT
    {
    }

    @Nested
    @Order(4)
    class NestedLinkIT extends LinkIT
    {
    }

    @Nested
    @Order(5)
    class NestedMacroIT extends MacroIT
    {
    }

    // CollaborationIT's multi-user scenario triggers a ClassCastException in DocumentUserReferenceModelSerializer
    // (superadmin isn't handled) when resolving a collaborator's user details, which the collaboration manager
    // doesn't catch. This can leave the shared browser/server session degraded, so this class must run last to
    // avoid poisoning any of the other nested test classes above.
    @Nested
    @Order(6)
    class NestedCollaborationIT extends CollaborationIT
    {
    }
}
