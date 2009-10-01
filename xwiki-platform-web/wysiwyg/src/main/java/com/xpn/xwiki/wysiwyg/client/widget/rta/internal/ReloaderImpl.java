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
package com.xpn.xwiki.wysiwyg.client.widget.rta.internal;

import java.util.Map;

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * Standard {com.xpn.xwiki.wysiwyg.client.widget.rta.Reloader} implementation.
 * 
 * @version $Id$
 */
public class ReloaderImpl
{
    /**
     * The rich text area to be reloaded.
     */
    private RichTextArea rta;

    /**
     * This object is used to preserve the list of inner HTML listeners while the rich text area is reloading. Rich text
     * area's document is renewed after each reload, the reference to the inner HTML listeners being thus lost along
     * with the old document.
     */
    private Object innerHTMLListeners;

    /**
     * Sets the rich text area that needs to be reloaded.
     * <p>
     * NOTE: We were forced to add this method because deferred binding uses only the default parameter-less
     * constructor.
     * 
     * @param rta a {@link RichTextArea}
     */
    public void setTextArea(RichTextArea rta)
    {
        if (this.rta == null) {
            this.rta = rta;
        } else {
            throw new IllegalStateException("Rich text area already set!");
        }
    }

    /**
     * @return the rich text area that is reloaded
     */
    public RichTextArea getTextArea()
    {
        return rta;
    }

    /**
     * Reloads the underlying rich text area.
     * 
     * @param params optional reload parameters
     * @param handler the object notified when the rich text area is reloaded
     */
    public void reload(Map<String, String> params, final LoadHandler handler)
    {
        final HandlerRegistration[] registrations = new HandlerRegistration[1];
        registrations[0] = rta.addLoadHandler(new LoadHandler()
        {
            public void onLoad(LoadEvent event)
            {
                registrations[0].removeHandler();
                // Restore the inner HTML listeners.
                swapInnerHTMLListeners();
                if (handler != null) {
                    handler.onLoad(event);
                }
            }
        });

        // Save the inner HTML listeners.
        swapInnerHTMLListeners();

        FormElement form = rta.getDocument().createFormElement();
        form.setAction("");
        form.setMethod("post");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            InputElement input = rta.getDocument().createHiddenInputElement();
            input.setName(entry.getKey());
            input.setValue(entry.getValue());
            form.appendChild(input);
        }

        rta.getDocument().getBody().appendChild(form);
        // The form is not submitted if the owner document is in design mode.
        rta.getDocument().setDesignMode(false);
        form.submit();
    }

    /**
     * Saves or restores the inner HTML listeners by swapping the value of {@link #innerHTMLListeners} with the value of
     * the {@code innerHTMLListeners} property of the document edited by the underlying rich text area.
     * <p>
     * NOTE: This method is more of a hack required to prevent loosing the inner HTML listeners when the rich text area
     * is reloaded. The inner HTML listeners are saved on the DOM document and thus are lost each time the in-line frame
     * used by the rich text area is reloaded because the document is renewed. We shouldn't be aware or depend on the
     * way the inner HTML listeners are managed by a DOM document but since it's not the responsibility of the document
     * to preserve its inner HTML listeners while the in-line frame reload we chose to use this hack.
     */
    protected void swapInnerHTMLListeners()
    {
        JavaScriptObject doc = rta.getDocument().cast();
        innerHTMLListeners = doc.set("innerHTMLListeners", innerHTMLListeners);
    }
}
