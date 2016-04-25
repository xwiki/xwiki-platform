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
package org.xwiki.gwt.wysiwyg.client.plugin.list.exec;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DefaultExecutable;

import com.google.gwt.dom.client.Node;

/**
 * List executable to insert a list (ordered or unordered). This will overwrite the default list insertion executable to
 * detect inserted list when using valid html for nested lists (by default, browsers fail to detect it properly).
 * 
 * @version $Id$
 */
public class ListExecutable extends DefaultExecutable
{
    /**
     * Stores whether the lists handled by this executable are ordered lists or not.
     */
    private boolean ordered;

    /**
     * Create a list executable to handle lists as specified by the parameter.
     * 
     * @param rta the execution target
     * @param ordered specified whether this executable handles ordered or unordered lists.
     */
    public ListExecutable(RichTextArea rta, boolean ordered)
    {
        super(rta, ordered ? Command.INSERT_ORDERED_LIST.toString() : Command.INSERT_UNORDERED_LIST.toString());
        this.ordered = ordered;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overwrite the default function to handle situations of valid HTML lists which are not detected correctly by the
     * browsers.
     * </p>
     * 
     * @see DefaultExecutable#isExecuted()
     */
    @Override
    public boolean isExecuted()
    {
        if (rta.getDocument().getSelection().getRangeCount() > 0) {
            Range range = rta.getDocument().getSelection().getRangeAt(0);
            Node rangeContainer = range.getCommonAncestorContainer();
            return (Element) DOMUtils.getInstance().getFirstAncestor(rangeContainer, ordered ? "ol" : "ul") != null;
        } else {
            return false;
        }
    }
}
