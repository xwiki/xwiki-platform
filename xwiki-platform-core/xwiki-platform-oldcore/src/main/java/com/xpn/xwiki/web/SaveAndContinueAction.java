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
package com.xpn.xwiki.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.csrf.CSRFToken;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action used for saving and returning to the edit page rather than viewing changes.
 *
 * @version $Id$
 */
public class SaveAndContinueAction extends XWikiAction
{

    /** Key for storing the wrapped action in the context. */
    private static final String WRAPPED_ACTION_CONTEXT_KEY = "SaveAndContinueAction.wrappedAction";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAndContinueAction.class);

    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return EditForm.class;
    }

    @Override
    protected String getName()
    {
        return "save";
    }

    /**
     * Write an error response to an ajax request.
     *
     * @param httpStatusCode The status code to set on the response.
     * @param message The message that should be displayed.
     * @param context the context.
     */
    private void writeAjaxErrorResponse(int httpStatusCode, String message, XWikiContext context)
    {
        try {
            context.getResponse().setContentType("text/plain");
            context.getResponse().setStatus(httpStatusCode);
            context.getResponse().setCharacterEncoding(context.getWiki().getEncoding());
            context.getResponse().getWriter().print(message);
        } catch (IOException e) {
            LOGGER.error("Failed to send error response to AJAX save and continue request.", e);
        }
    }

    /**
     * Perform the internal action implied by the save and continue request. If the request is an ajax request,
     * writeAjaxErrorResponse will be called. The return value will be that of the wrapped action.
     *
     * @param isAjaxRequest Indicate if this is an ajax request.
     * @param back The back URL.
     * @param context The xwiki context.
     * @return {\code false} if the request is an ajax request, otherwise the return value of the wrapped action.
     * @throws XWikiException
     */
    private boolean doWrappedAction(boolean isAjaxRequest, String back, XWikiContext context) throws XWikiException
    {

        boolean failure = false;

        // This will never be true if "back" comes from request.getHeader("referer")
        if (back != null && back.contains("editor=class")) {
            PropUpdateAction pua = new PropUpdateAction();

            if (pua.propUpdate(context)) {
                if (isAjaxRequest) {
                    String errorMessage = localizePlainOrKey((String) context.get("message"));
                    writeAjaxErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage, context);
                } else {
                    context.put(WRAPPED_ACTION_CONTEXT_KEY, pua);
                }

                failure = true;
            }
        } else {
            SaveAction sa = new SaveAction();
            if (sa.save(context)) {
                if (isAjaxRequest) {
                    String errorMessage =
                        localizePlainOrKey("core.editors.saveandcontinue.theDocumentWasNotSaved");
                    // This should not happen. SaveAction.save(context) should normally throw an
                    // exception when failing during save and continue.
                    LOGGER.error("SaveAction.save(context) returned true while using save & continue");
                    writeAjaxErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage, context);
                } else {
                    context.put(WRAPPED_ACTION_CONTEXT_KEY, sa);
                }

                failure = true;
            } else {
                // Lock back the document
                context.getDoc().getTranslatedDocument(context).setLock(context.getUser(), context);
            }
        }

        return failure;
    }

    /**
     * @param isAjaxRequest Indicate if this is an ajax request.
     * @param context The XWiki context.
     * @throws XWikiException unless it is an ajax request.
     */
    private void handleCSRFValidationFailure(boolean isAjaxRequest, XWikiContext context)
        throws XWikiException
    {
        final String csrfCheckFailedMessage = localizePlainOrKey("core.editors.saveandcontinue.csrfCheckFailed");
        if (isAjaxRequest) {
            writeAjaxErrorResponse(HttpServletResponse.SC_FORBIDDEN,
                csrfCheckFailedMessage,
                context);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID,
                csrfCheckFailedMessage);
        }
    }

    /**
     * @param isAjaxRequest Indicate if this is an ajax request.
     * @param exception The exception to handle.
     * @param context The XWiki context.
     * @throws XWikiException unless it is an ajax request.
     */
    private void handleException(boolean isAjaxRequest, Exception exception, XWikiContext context)
        throws XWikiException
    {
        if (isAjaxRequest) {
            String errorMessage =
                localizePlainOrKey("core.editors.saveandcontinue.exceptionWhileSaving", exception.getMessage());

            writeAjaxErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage, context);

            String logMessage = "Caught exception during save and continue";
            if (exception instanceof XWikiException) {
                LOGGER.info(logMessage, exception);
            } else {
                LOGGER.error(logMessage, exception);
            }
        } else {
            if (exception instanceof XWikiException) {
                throw (XWikiException) exception;
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception", exception);
            }
        }
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        CSRFToken csrf = Utils.getComponent(CSRFToken.class);
        String token = context.getRequest().getParameter("form_token");

        // If the request is an ajax request, we will:
        //
        // 1) _not_ send a redirect response
        //
        // 2) if for any reason the document is not saved, call the method writeAjaxErrorResponse and return false
        // (which normally indicates success).

        final boolean isAjaxRequest = Utils.isAjaxRequest(context);

        if (!csrf.isTokenValid(token)) {
            handleCSRFValidationFailure(isAjaxRequest, context);
            return false;
        }

        // Try to find the URL of the edit page which we came from
        String back = findBackURL(context);

        try {
            if (doWrappedAction(isAjaxRequest, back, context)) {
                return !isAjaxRequest;
            }
        } catch (Exception e) {
            handleException(isAjaxRequest, e, context);
            return !isAjaxRequest;
        }

        // If this is an ajax request, no need to redirect.
        if (isAjaxRequest) {
            context.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }

        // Forward back to the originating page
        try {
            context.getResponse().sendRedirect(back);
        } catch (IOException ignored) {
            // This exception is ignored because it will only be thrown if content has already been sent to the
            // response. This should never happen but we have to catch the exception anyway.
        }
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiAction wrappedAction = (XWikiAction) context.get(WRAPPED_ACTION_CONTEXT_KEY);

        if (wrappedAction != null) {
            return wrappedAction.render(context);
        }

        return "exception";
    }

    /**
     * Try to find the URL of the edit page which we came from.
     *
     * @param context current xwiki context
     * @return URL of the edit page
     */
    private String findBackURL(XWikiContext context)
    {
        XWikiRequest request = context.getRequest();
        String back = request.getParameter("xcontinue");
        if (StringUtils.isEmpty(back)) {
            back = request.getParameter("xredirect");
        }
        if (StringUtils.isEmpty(back)) {
            back = removeAllParametersFromQueryStringExceptEditor(request.getHeader("Referer"));
        }
        if (StringUtils.isEmpty(back)) {
            back = context.getDoc().getURL("edit", context);
        }
        return back;
    }

    /**
     * @param url the URL to get a modified version of.
     * @return A modified version of the input url where all parameters are stripped from the query string except
     *         "editor"
     */
    private String removeAllParametersFromQueryStringExceptEditor(String url)
    {
        if (url == null) {
            return "";
        }

        String[] baseAndQuery = url.split("\\?");
        // No query string: no change.
        if (baseAndQuery.length < 2) {
            return url;
        }

        String[] queryBeforeAndAfterEditor = baseAndQuery[1].split("editor=");
        // No editor=* in query string: return URI
        if (queryBeforeAndAfterEditor.length < 2) {
            return baseAndQuery[0];
        }

        return baseAndQuery[0] + "?editor=" + queryBeforeAndAfterEditor[1].split("&")[0];
    }
}
