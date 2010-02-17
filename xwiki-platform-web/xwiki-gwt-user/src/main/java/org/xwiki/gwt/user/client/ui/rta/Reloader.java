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

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
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
        this.rta = rta;

        requestBuilder = new RequestBuilder(RequestBuilder.POST, IFrameElement.as(rta.getElement()).getSrc());
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
            request = requestBuilder.sendRequest(serialize(params), this);
            // Update the call-back if the reload request was successfully sent.
            this.callback = callback;
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }

    /**
     * Serializes the given parameters in order to be send with a {@code post} HTTP request that has the {@code
     * Content-type} header set to {@code application/x-www-form-urlencoded}.
     * 
     * @param params the parameters to be serialized
     * @return a string that can be used as post data
     */
    private String serialize(Map<String, String> params)
    {
        StringBuffer postData = new StringBuffer();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            postData.append(URL.encodeComponent(entry.getKey())).append('=').append(
                URL.encodeComponent(entry.getValue())).append('&');
        }
        return postData.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see RequestCallback#onError(Request, Throwable)
     */
    public void onError(Request request, Throwable exception)
    {
        this.request = null;
        callback.onFailure(exception);
        callback = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see RequestCallback#onResponseReceived(Request, Response)
     */
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
                loadRegistration = rta.addLoadHandler(this);
                // We can't reload the document while it is in design mode.
                rta.getDocument().setDesignMode(false);
            }
            updateContent(response.getText());
        } else {
            onError(null, new RuntimeException(response.getStatusCode() == 0 ? Strings.INSTANCE
                .httpStatusTextRequestAborted() : response.getStatusText()));
        }
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

    /**
     * {@inheritDoc}
     * 
     * @see LoadHandler#onLoad(LoadEvent)
     */
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
