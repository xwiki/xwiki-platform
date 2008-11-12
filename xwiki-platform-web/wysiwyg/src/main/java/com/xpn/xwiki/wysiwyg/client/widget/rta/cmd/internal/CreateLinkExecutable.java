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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.google.gwt.core.client.GWT;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Creates a link by inserting the link xhtml.
 * 
 * @version $Id$
 */
public class CreateLinkExecutable implements Executable
{
    /**
     * Insert HTML executable to use for the insertion of the links. Cannot extend it because we need to get it in a
     * cross browser manner.
     */
    private Executable insertHTMLExecutable;

    /**
     * Default constructor, creating a {@link DefaultExecutable} for the <code>inserthtml</code> command.
     */
    public CreateLinkExecutable()
    {
        // Create the InsertHTML cross-browser executable
        insertHTMLExecutable = GWT.create(InsertHTMLExecutable.class);
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        return insertHTMLExecutable.execute(rta, parameter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        String anchorTagName = "a";
        // This option is enabled only if we're not in another or the selection does not touch an anchor
        Range range = DOMUtils.getInstance().getTextRange(rta.getDocument().getSelection().getRangeAt(0));
        //Check the parent first, for it's shorter
        if (DOMUtils.getInstance().getFirstAncestor(range.getCommonAncestorContainer(), anchorTagName) != null) {
            return false;
        }
        // if no anchor on ancestor, test all the nodes touched by the selection to not contain an anchor
        return DOMUtils.getInstance().getFirstDescendant(range.cloneContents(), anchorTagName) == null;        
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        // Although we should also check the parents of the anchor we are in
        return !isEnabled(rta);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSupported(RichTextArea rta)
    {
        return insertHTMLExecutable.isSupported(rta);
    }

    /**
     * {@inheritDoc}
     */
    public String getParameter(RichTextArea rta)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
