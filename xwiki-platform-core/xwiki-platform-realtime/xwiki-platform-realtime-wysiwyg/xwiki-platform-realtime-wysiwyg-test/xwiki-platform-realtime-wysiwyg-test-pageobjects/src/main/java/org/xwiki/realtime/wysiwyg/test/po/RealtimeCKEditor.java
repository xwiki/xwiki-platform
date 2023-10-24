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
package org.xwiki.realtime.wysiwyg.test.po;

import org.xwiki.ckeditor.test.po.CKEditor;

/**
 * Represents the realtime CKEditor.
 * 
 * @version $Id$
 * @since 14.10.19
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeCKEditor extends CKEditor
{
    /**
     * Default constructor.
     */
    public RealtimeCKEditor()
    {
        super("content");
        waitToLoad();
    }

    @Override
    public CKEditor waitToLoad()
    {
        super.waitToLoad();
        return this;
    }

    @Override
    public RealtimeRichTextAreaElement getRichTextArea()
    {
        // The in-line frame element is renewed while editing so we can't cache it.
        return new RealtimeRichTextAreaElement(getIframe());
    }

    @Override
    public RealtimeCKEditorToolBar getToolBar()
    {
        return new RealtimeCKEditorToolBar(this);
    }
}
