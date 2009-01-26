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
package com.xpn.xwiki.wysiwyg.client.syntax.rule;

import com.xpn.xwiki.wysiwyg.client.plugin.table.util.TableUtils;
import com.xpn.xwiki.wysiwyg.client.syntax.ValidationRule;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Validation rule used to define the behavior when the selection is in a table. This will disable features that refer
 * to block elements such as titles, lists, separator, etc.
 * 
 * @version $Id$
 */
public class DisableBlockElementsInTable implements ValidationRule
{
    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(RichTextArea)
     */
    public boolean areValid(RichTextArea rta)
    {
        // Listed features are valid if the selection is located outside of a table.
        return TableUtils.getInstance().getTable(TableUtils.getInstance().getCaretNode(rta.getDocument())) == null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        // List of block elements commands.
        return new String[] {"unorderedlist", "orderedlist", "outdent", "indent", "format", "hr"};
    }

}
