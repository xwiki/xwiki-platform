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

import com.xpn.xwiki.wysiwyg.client.dom.internal.IERangeFactory;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.ControlRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.NativeRange;
import com.xpn.xwiki.wysiwyg.client.dom.internal.ie.TextRange;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * This is a custom implementation of the predefined command "insertHTML" made because Internet Explorer doesn't support
 * it.
 * 
 * @version $Id$
 */
public class IEInsertHTMLExecutable implements Executable
{
    /**
     * {@inheritDoc}
     * 
     * @see Executable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String param)
    {
        if (isEnabled(rta)) {
            NativeRange nativeRange = IERangeFactory.cast(rta.getDocument().getSelection().getRangeAt(0)).getJSRange();
            if (nativeRange.isTextRange()) {
                ((TextRange) nativeRange).setHTML(param);
            } else {
                TextRange textRange = TextRange.newInstance((ControlRange) nativeRange);
                textRange.select();
                textRange.setHTML(param);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return rta.getDocument().getSelection().getRangeCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Executable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return true;
    }
}
