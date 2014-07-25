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
package org.xwiki.gwt.user.client.ui.rta;

import java.util.Map;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.IFrameElement;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.user.client.Strings;
import org.xwiki.gwt.user.client.URLUtils;
import org.xwiki.gwt.user.client.ui.rta.internal.ReloaderImpl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Reloads a rich text area.
 * 
 * @version $Id$
 */
public class Reloader implements RequestCallback, LoadHandler
{
    /**
     * The rich text area to be reloaded.
     */
    private final RichTextArea rta;

    /**
     * The object used to do browser-specific reload operations.
     */
    private final ReloaderImpl impl = GWT.create(ReloaderImpl.class);

    /**
     * This object is used to preserve the list of inner HTML listeners while the rich text area is reloading. Rich text
     * area's document is renewed after each reload, the reference to the inner HTML listeners being thus lost along
     * with the old document.
     */
    private Object innerHTMLListeners;

    /**
     * The object notified when the reload ends.
     */
    private AsyncCallback< ? > callback;

    /**
     * The object used to send reload requests.
     */
    private final RequestBuilder requestBuilder;

    /**
     * The current reload request.
     */
    private Request request;

    /**
     * Registration for rich text area's load event.
     */
    private HandlerRegistration loadRegistration;

    /**
     * Creates a new reloader for the specified rich text area.
     * 
     * @param rta the rich text that needs to be reloaded
     */
    public Reloader(RichTextArea rta)
    {
        this(rta, IFrameElement.as(rta.getElement()).getSrc());
    }

    /**
     * Creates a new reloader that makes requests to the specified URL and uses the response to reset the content of the
     * given rich text area.
     * 
     * @param rta the rich text area that needs to be reloaded
     * @param url the URL to get the content from; note that this URL must obey the same-origin policy
     */
    public Reloader(RichTextArea rta, String url)
    {
        this.rta = rta;

        requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
        // NOTE: We must specify the character set because otherwise the server side will use the configured encoding.
        requestBuilder.setHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    /**
     * Reloads the underlying rich text area.
     * 
     * @param params optional reload parameters
     * @param callback the object notified when the rich text area is reloaded
     */
    public void reload(Map<String, String> params, AsyncCallback< ? > callback)
    {
        // Cancel the pending request.
        if (request != null) {
            request.cancel();
            request = null;
            this.callback = null;
        }
        try {
            // Make a new reload request.
            request = requestBuilder.sendRequest(URLUtils.serializeQueryStringParameters(params), this);
            // Update the call-back if the reload request was successfully sent.
            this.callback = callback;
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }

    @Override
    public void onError(Request request, Throwable exception)
    {
        this.request = null;
        callback.onFailure(exception);
        callback = null;
    }

    @Override
    public void onResponseReceived(Request request, Response response)
    {
        this.request = null;
        // Check if the request completed with success.
        if (response.getStatusCode() == Response.SC_OK) {
            // If the rich text area is currently loading, stop it.
            if (loadRegistration != null) {
                ((IFrameElement) rta.getElement().cast()).getContentWindow().stop();
            } else {
                // Save the inner HTML listeners.
                swapInnerHTMLListeners();
                // Renew the in-line frame used by the rich text area in order to prevent the browser from recording a
                // new entry in its navigation history. This is a hack but we had no choice.
                renewRichTextAreaElement();
                // Listen to the load event to restore the inner HTML listeners.
                loadRegistration = rta.addLoadHandler(this);
            }
            updateContent(response.getText());
        } else {
            onError(null, new RuntimeException(response.getStatusCode() == 0 ? Strings.INSTANCE
                .httpStatusTextRequestAborted() : response.getStatusText()));
        }
    }

    /**
     * Renew the in-line frame used by the rich text area in order to prevent the browser from recording a new entry in
     * its navigation history. This is a hack but we had no choice.
     */
    private native void renewRichTextAreaElement()
    /*-{
        var rta = this.@org.xwiki.gwt.user.client.ui.rta.Reloader::rta;

        // Fake detach to release the event listeners.
        rta.@com.google.gwt.user.client.ui.Widget::onDetach()();

        // Renew the in-line frame element.
        var newIFrame = this.@org.xwiki.gwt.user.client.ui.rta.Reloader::renewIFrameElement()();

        // Update the reference to the rich text area element.
        rta.@com.google.gwt.user.client.ui.UIObject::element = newIFrame;
        var rtaImpl = rta.@com.google.gwt.user.client.ui.RichTextArea::impl;
        rtaImpl.@com.google.gwt.user.client.ui.impl.RichTextAreaImpl::elem = newIFrame;
        
        // Fake attach to initialize the event listeners.
        rta.@com.google.gwt.user.client.ui.Widget::onAttach()();
    }-*/;

    /**
     * Renews the in-line frame element used by the rich text area.
     * 
     * @return the new in-line frame element
     * @see #renewRichTextAreaElement()
     */
    private IFrameElement renewIFrameElement()
    {
        IFrameElement iFrame = IFrameElement.as(rta.getElement()).cast();
        Node parent = iFrame.getParentNode();
        Node nextSibling = iFrame.getNextSibling();

        impl.unloadIFrameElement(iFrame);

        // The listener is not removed when onDetach is called. See RichTextArea#onDetach() for details.
        DOM.setEventListener((com.google.gwt.user.client.Element) iFrame.cast(), null);

        // Clone the previous in-line frame.
        IFrameElement newIFrame = (IFrameElement) iFrame.cloneNode(false);

        // Attach the new in-line frame to the DOM.
        if (nextSibling != null) {
            parent.insertBefore(newIFrame, nextSibling);
        } else {
            parent.appendChild(newIFrame);
        }

        // Don't let the in-line frame load to prevent the browser from recording a new history entry. We use DOMUtils
        // because only static references to overlay types are allowed from JSNI.
        newIFrame.getContentWindow().stop();

        // onAttach sets the listener only the first time it is called. See RichTextArea#onAttach() for details.
        DOM.setEventListener((com.google.gwt.user.client.Element) iFrame.cast(), rta);

        return newIFrame;
    }

    /**
     * Updates the content of the rich text area.
     * 
     * @param content the new content
     */
    private void updateContent(String content)
    {
        // We have previously used the JavaScript URI scheme/protocol to update the content of the rich text area but we
        // had problems on IE when using the Unicode character set with UTF-8 encoding. It seems IE doesn't detect the
        // correct encoding when using the JavaScript URI scheme unless we add a byte order mark (\uFEFF) at the start
        // of the content.
        Document document = rta.getDocument();
        document.open();
        document.write(content);
        document.close();
    }

    @Override
    public void onLoad(LoadEvent event)
    {
        loadRegistration.removeHandler();
        loadRegistration = null;
        // Restore the inner HTML listeners.
        swapInnerHTMLListeners();
        // Don't notify the success if a new reload request has been made while the rich text area was loading.
        if (request == null) {
            callback.onSuccess(null);
            callback = null;
        }
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
    private void swapInnerHTMLListeners()
    {
        JavaScriptObject doc = rta.getDocument().cast();
        innerHTMLListeners = doc.set("innerHTMLListeners", innerHTMLListeners);
    }
}
