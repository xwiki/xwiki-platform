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
package org.xwiki.gwt.dom.client.internal.ie;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.internal.DefaultSelection;
import org.xwiki.gwt.dom.client.internal.mozilla.NativeSelection;

/**
 * Although Internet Explorer 9 implements the Selection and Range API it still doesn't support separate selections per
 * window. If you have an in-line frame, any selection you make inside it will be lost when you focus its parent window.
 * The child and parent windows have different selection objects but these objects can't have rangeCount greater than 0
 * at the same time. If one of them contains a range, the other is empty.
 * 
 * @version $Id$
 */
public class IESelection extends DefaultSelection
{
    /**
     * The native selection used when the window is not focused.
     */
    private NativeSelection fakeNativeSelection;

    /**
     * Creates a new instance that can be used to control the selection associated with the given document.
     * 
     * @param document a DOM document
     */
    public IESelection(Document document)
    {
        super(NativeSelection.getInstance(document));
        fakeNativeSelection = getFakeNativeSelection(document);
    }

    @Override
    protected NativeSelection getNativeSelection()
    {
        if (fakeNativeSelection.getRangeCount() > 0) {
            return fakeNativeSelection;
        } else {
            return super.getNativeSelection();
        }
    }

    /**
     * @param document a DOM document
     * @return a fake native selection object that can be used to read the selection of the given document while its
     *         window doesn't have the focus
     */
    private native NativeSelection getFakeNativeSelection(Document document)
    /*-{
        var view = document.defaultView;
        if (!view.__fakeSelection) {
            view.__fakeSelection = {
                rangeCount : 0,
                addRange : function(range) {
                    this.__savedRange = range;
                    this.rangeCount = 1;
                },
                getRangeAt : function(index) {
                    return this.__savedRange;
                },
                removeAllRanges : function() {
                    this.__savedRange = undefined;
                    this.rangeCount = 0;
                },
                removeRange : function(range) {
                    if (range == this.getRangeAt(0)) {
                        this.removeAllRanges();
                    }
                },
                __save : function() {
                    if (view.getSelection().rangeCount > 0) {
                        this.addRange(view.getSelection().getRangeAt(0));
                    }
                },
                __restore : function() {
                    if (this.rangeCount > 0) {
                        view.getSelection().removeAllRanges();
                        view.getSelection().addRange(this.getRangeAt(0));
                        this.removeAllRanges();
                    }
                }
            };
            var focusInListener = function() {
                view.__fakeSelection.__restore();
            };
            var focusOutListener = function() {
                view.__fakeSelection.__save();
            };
            var unloadListener = function() {
                view.removeEventListener('focusin', focusInListener, false);
                view.removeEventListener('focusout', focusOutListener, false);
                view.removeEventListener('unload', arguments.callee, false);
                view = view.__fakeSelection = focusInListener = focusOutListener = undefined;
            }
            view.addEventListener('focusin', focusInListener, false);
            view.addEventListener('focusout', focusOutListener, false);
            view.addEventListener('unload', unloadListener, false);
        }
        return view.__fakeSelection;
    }-*/;
}
