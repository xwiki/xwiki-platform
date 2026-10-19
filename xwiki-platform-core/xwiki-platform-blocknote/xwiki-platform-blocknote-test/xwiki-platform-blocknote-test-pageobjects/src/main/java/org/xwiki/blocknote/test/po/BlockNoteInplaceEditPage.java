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
package org.xwiki.blocknote.test.po;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.realtime.test.po.RealtimeInplaceEditablePage;

/**
 * Represents the in-place edit mode when the page content is edited in realtime with BlockNote.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class BlockNoteInplaceEditPage extends RealtimeInplaceEditablePage
{
    /**
     * Navigate to the specified page, in view mode, from where it can be edited in-place.
     *
     * @param pageReference the page to navigate to
     * @return the in-place editable page
     */
    public static BlockNoteInplaceEditPage gotoPage(EntityReference pageReference)
    {
        getUtil().gotoPage(pageReference);
        return new BlockNoteInplaceEditPage();
    }

    @Override
    public BlockNoteInplaceEditPage editInplace()
    {
        super.editInplace();
        return this;
    }

    @Override
    public BlockNoteInplaceEditPage waitForInplaceEditor()
    {
        super.waitForInplaceEditor();
        return this;
    }

    /**
     * @return the editor used to edit the page content
     */
    public BlockNoteEditor getContentEditor()
    {
        return new BlockNoteEditor("content");
    }
}
