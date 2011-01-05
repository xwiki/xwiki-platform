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
package org.xwiki.gwt.wysiwyg.client.plugin.table.feature;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePlugin;

import com.google.gwt.dom.client.Node;

/**
 * Feature allowing to remove a table from the editor. It is disabled when the caret is positioned outside of a table
 * element.
 * 
 * @version $Id$
 */
public class DeleteTable extends AbstractTableFeature
{
    /**
     * Feature name.
     */
    public static final String NAME = "deletetable";

    /**
     * The name of the table HTML element.
     */
    private static final String TABLE = "table";

    /**
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public DeleteTable(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), Strings.INSTANCE.deleteTable(), plugin);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractTableFeature#execute(String)
     */
    public boolean execute(String parameter)
    {
        Selection selection = rta.getDocument().getSelection();
        Node table = domUtils.getFirstAncestor(selection.getRangeAt(0).getCommonAncestorContainer(), TABLE);
        Node previousLeaf = domUtils.getPreviousLeaf(table);
        Node parent = table.getParentNode();

        selection.removeAllRanges();
        parent.removeChild(table);

        Range range = rta.getDocument().createRange();
        if (previousLeaf != null) {
            if (previousLeaf.getNodeType() == Node.ELEMENT_NODE && Element.as(previousLeaf).canHaveChildren()) {
                range.setStart(previousLeaf, 0);
            } else {
                range.setStartAfter(previousLeaf);
            }
        } else {
            range.setStart(parent, 0);
        }
        range.collapse(true);
        selection.addRange(range);

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractTableFeature#isEnabled()
     */
    public boolean isEnabled()
    {
        return super.isEnabled()
            && domUtils.getFirstAncestor(rta.getDocument().getSelection().getRangeAt(0).getCommonAncestorContainer(),
                TABLE) != null;
    }
}
