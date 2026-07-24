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
package org.xwiki.model.validation.edit;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.validation.edit.internal.EditConfirmationCheckersManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link EditConfirmationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class EditConfirmationScriptServiceTest
{
    @InjectMockComponents
    private EditConfirmationScriptService scriptService;

    @MockComponent
    private EditConfirmationCheckersManager editConfirmationCheckersManager;

    @Test
    void check()
    {
        EditConfirmationCheckerResults results = new EditConfirmationCheckerResults();
        when(this.editConfirmationCheckersManager.check()).thenReturn(results);

        assertSame(results, this.scriptService.check());
        verify(this.editConfirmationCheckersManager).check();
    }

    @Test
    void checkWithSkipHints()
    {
        EditConfirmationCheckerResults results = new EditConfirmationCheckerResults();
        when(this.editConfirmationCheckersManager.check(Set.of("documentLock"))).thenReturn(results);

        // The script service accepts any collection (e.g. a Velocity list) and forwards it as a set.
        assertSame(results, this.scriptService.check(List.of("documentLock")));
        verify(this.editConfirmationCheckersManager).check(Set.of("documentLock"));
    }

    @Test
    void force()
    {
        this.scriptService.force();
        verify(this.editConfirmationCheckersManager).force();
    }
}
