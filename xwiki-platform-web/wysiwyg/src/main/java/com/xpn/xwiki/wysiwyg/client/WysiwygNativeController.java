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
package com.xpn.xwiki.wysiwyg.client;

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.user.client.Element;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditor;

/**
 * Utility class used for creating a native JavaScript controller for the WYSIWYG editors.
 * <p>
 * NOTE: We were forced to move the native controller code out of the {@link Wysiwyg} class because apparently
 * referencing the entry point class from native JavaScript code causes an exception at compile time just before
 * initializing the hosted mode.
 * 
 * @version $Id$
 */
public final class WysiwygNativeController
{
    /**
     * This is a utility class and thus has a private constructor.
     */
    private WysiwygNativeController()
    {
    }

    /**
     * Extends the given {@link JavaScriptObject} with methods for accessing and controlling the WYSIWYG editors from
     * native JavaScript code. The following methods are added:
     * <ul>
     * <li>getPlainTextArea(String): get the HTML text area for the editor with the given id</li>
     * <li>getRichTextArea(String): get the in-line frame for the editor with the given id.</li>
     * </ul>
     * 
     * @param object the JavaScript object to extend
     */
    public static native void extend(JavaScriptObject object)
    /*-{
        object.getPlainTextArea = function(id) {
            return @com.xpn.xwiki.wysiwyg.client.WysiwygNativeController::getPlainTextArea(Ljava/lang/String;)(id);
        };
        object.getRichTextArea = function(id) {
            return @com.xpn.xwiki.wysiwyg.client.WysiwygNativeController::getRichTextArea(Ljava/lang/String;)(id);
        };
    }-*/;

    /**
     * Get the plain HTML text area element for the editor with the given ID.
     * 
     * @param id the editor identifier, usually the id of the plain text area wrapped by the WYSIWYG editor
     * @return the plain HTML text area element for the editor with the given id, or {@code null} is there's no editor
     *         with the given id
     */
    protected static Element getPlainTextArea(String id)
    {
        WysiwygEditor editor = Wysiwyg.getEditor(id);
        return editor == null ? null : editor.getPlainTextEditor().getTextArea().getElement();
    }

    /**
     * Get the rich text area element, {@code iframe}, for the editor with the given id.
     * 
     * @param id the editor identifier, usually the id of the plain text area wrapped by the WYSIWYG editor
     * @return the rich text area element for the editor with the given id, or {@code null} is there's no editor with
     *         the given id
     */
    protected static Element getRichTextArea(String id)
    {
        WysiwygEditor editor = Wysiwyg.getEditor(id);
        return editor == null ? null : editor.getRichTextEditor().getTextArea().getElement();
    }
}
