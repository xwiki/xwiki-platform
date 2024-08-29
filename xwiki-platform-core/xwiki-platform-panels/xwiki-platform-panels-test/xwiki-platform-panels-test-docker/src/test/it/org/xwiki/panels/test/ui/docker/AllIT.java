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
package org.xwiki.panels.test.ui.docker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI Tests for the Panels.
 *
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
class AllIT
{
    @Nested
    @DisplayName("Panels Administration Tests")
    class NestedPanelsAdministrationIT extends PanelsAdministrationIT
    {
    }

    @Nested
    @DisplayName("Navigation Panel Tests")
    class NestedNavigationPanelIT extends NavigationPanelIT
    {
    }

    @Nested
    @DisplayName("Navigation Panel Administration Tests")
    class NestedNavigationPanelAdministrationIT extends NavigationPanelAdministrationIT
    {
    }

    @Nested
    @DisplayName("Panels Tests")
    class NestedPanelIT extends PanelIT
    {
    }

    @Nested
    @DisplayName("Application Panels Administration Tests")
    class NestedApplicationsPanelAdministrationIT extends ApplicationsPanelAdministrationIT
    {
    }

    @Nested
    @DisplayName("New Page Panel Tests")
    class NestedNewPagePanelIT extends NewPagePanelIT
    {
    }
}
