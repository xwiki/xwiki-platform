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
package org.xwiki.livedata;

import java.util.Map;

import org.xwiki.stability.Unstable;

/**
 * The properties required to execute an action asynchrously. This includes status messages (loading, success,
 * failure...) as well as technical http client parameters to for developers to adapt the requests to their own needs.
 *
 * @version $Id$
 * @since 16.2.0RC1
 */
@Unstable
public class LiveDataAsyncActionDescriptor
{
    private String loadingMessage;

    private String successMessage;

    private String failureMessage;

    private String confirmationMessage;

    private String httpMethod;

    private String body;

    private Map<String, String> headers;

    /**
     * @return the localized async action loading message
     */
    public String getLoadingMessage()
    {
        return this.loadingMessage;
    }

    /**
     * @param loadingMessage the localized async action loading message
     */
    public void setLoadingMessage(String loadingMessage)
    {
        this.loadingMessage = loadingMessage;
    }

    /**
     * @return the localized async action success message
     */
    public String getSuccessMessage()
    {
        return this.successMessage;
    }

    /**
     * @param successMessage the localized async action success message
     */
    public void setSuccessMessage(String successMessage)
    {
        this.successMessage = successMessage;
    }

    /**
     * @return the localized async action error message
     */
    public String getFailureMessage()
    {
        return this.failureMessage;
    }

    /**
     * @param failureMessage the localized async action error message
     */
    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }

    /**
     * @return the http method to use when performing the asynchronous action
     */
    public String getHttpMethod()
    {
        return this.httpMethod;
    }

    /**
     * @param httpMethod the http method to use when performing the asynchronous action
     */
    public void setHttpMethod(String httpMethod)
    {
        this.httpMethod = httpMethod;
    }

    /**
     * @return an optional confirmation message to ask for confirmation before performing the action
     */
    public String getConfirmationMessage()
    {
        return this.confirmationMessage;
    }

    /**
     * @param confirmationMessage an optional confirmation message to ask for confirmation before performing the
     *     action
     */
    public void setConfirmationMessage(String confirmationMessage)
    {
        this.confirmationMessage = confirmationMessage;
    }

    /**
     * @return an optional request body
     */
    public String getBody()
    {
        return this.body;
    }

    /**
     * @param body an optional request body
     */
    public void setBody(String body)
    {
        this.body = body;
    }

    /**
     * @return an optional map of headers to pass to the request
     */
    public Map<String, String> getHeaders()
    {
        return this.headers;
    }

    /**
     * @param headers an optional map of headers to pass to the request
     */
    public void setHeaders(Map<String, String> headers)
    {
        this.headers = headers;
    }
}
