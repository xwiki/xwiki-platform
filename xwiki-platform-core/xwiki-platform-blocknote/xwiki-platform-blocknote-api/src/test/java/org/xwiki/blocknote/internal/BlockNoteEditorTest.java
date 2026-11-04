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
package org.xwiki.blocknote.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.edit.Editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BlockNoteEditor}.
 *
 * @version $Id$
 */
class BlockNoteEditorTest
{
    @Test
    void hasLowerPriorityThanTheDefault()
    {
        List<ComponentDescriptor<Editor<?>>> descriptors =
            new ComponentDescriptorFactory().createComponentDescriptors(BlockNoteEditor.class, Editor.class);

        assertEquals(1, descriptors.size());
        ComponentDescriptor<Editor<?>> descriptor = descriptors.get(0);
        assertEquals(BlockNoteEditor.ROLE_HINT, descriptor.getRoleHint());
        // The editor manager falls back to the first editor of the WYSIWYG category when none is configured, so
        // BlockNote must rank after the editors registered with the default priority, such as CKEditor.
        assertTrue(descriptor.getRoleTypePriority() > ComponentDescriptor.DEFAULT_PRIORITY,
            "BlockNote must not win the implicit default WYSIWYG editor");
    }
}
