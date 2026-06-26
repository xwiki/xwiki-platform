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
package org.xwiki.ckeditor.test.ui;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All functional tests for the CKEditor integration.
 *
 * @version $Id$
 * @since 1.53.2
 */
@UITest
public class AllIT
{
    @Nested
    class NestedSaveIT extends SaveIT
    {
    }

    @Nested
    class NestedImageIT extends ImageIT
    {
    }

    @Nested
    class NestedTextAreaIT extends TextAreaIT
    {
    }

    @Nested
    class NestedLinkIT extends LinkIT
    {
    }

    @Nested
    class NestedQuickActionsIT extends QuickActionsIT
    {
    }

    @Nested
    class NestedUndoRedoIT extends UndoRedoIT
    {
    }

    @Nested
    class NestedLocalizationIT extends LocalizationIT
    {
    }

    @Nested
    class NestedFilterIT extends FilterIT
    {
    }

    @Nested
    class NestedTableIT extends TableIT
    {
    }
}
