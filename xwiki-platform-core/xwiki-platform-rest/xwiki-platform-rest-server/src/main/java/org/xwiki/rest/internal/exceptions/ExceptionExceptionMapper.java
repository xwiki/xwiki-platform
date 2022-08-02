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
package org.xwiki.rest.internal.exceptions;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Exception mapper for all {@link Exception} exceptions. Allowing to log all exceptions that are not inheriting from
 * {@link RuntimeException} or already mapped by another exception mapper.
 * <p>
 * The goal is to be able to have access to the stacktrace of the exceptions, instead of a simple http 500 error and a
 * {@code "No ExceptionMapper was found, but must be found"} message (see
 * <a href="https://github.com/restlet/restlet-framework-java/issues/1070">
 * https://github.com/restlet/restlet-framework-java/issues/1070</a>).
 *
 * @version $Id$
 * @since 13.8RC1
 * @since 13.7.1
 * @since 13.4.4
 */
@Component
@Named("org.xwiki.rest.internal.exceptions.ExceptionExceptionMapper")
@Provider
@Singleton
public class ExceptionExceptionMapper implements ExceptionMapper<Exception>, XWikiRestComponent
{
    @Inject
    private Logger logger;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Override
    public Response toResponse(Exception cause)
    {
        // This ExceptionMapper captures too many exception types, because we can't distinguish checked and unchecked
        // exceptions using generic types. If the mapped type is RuntimeException (unchecked exception), JaxRsProviders
        // would re-throw the exception without mapping it (or it would have been handled by another mapper instead of
        // this one), we reproduce this behavior to stay as close as possible to the default implementation (note:
        // RuntimeException stacktraces are logged by default).
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }

        // Logs the unmapped exception, with its stacktrace.
        this.logger.error("A REST endpoint failed with an unmapped exception.", cause);

        // Returns an error response to the client, with some detail about the exception in the response.
        String message;
        String templateName = "rest/exception.vm";
        MediaType mediaType;
        try {
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            scriptContext.setAttribute("cause", cause, ScriptContext.ENGINE_SCOPE);
            // The message generated from the template is in HTML. 
            mediaType = MediaType.TEXT_HTML_TYPE;
            message = this.templateManager.render(templateName);
        } catch (Exception e) {
            this.logger.warn("Failed to render the response using template [{}]. Cause: [{}].", templateName,
                getRootCauseMessage(e));
            // Fallback to a formatted string when the template rendering fails.
            // In case of failure when generating the message content from a template, a text content is generated 
            // instead.
            String translation =
                this.contextLocalization.getTranslationPlain("rest.exception.noMapper", cause.getClass().getName());
            mediaType = TEXT_PLAIN_TYPE;
            // We use a hardcoded \n because the message is used in an HTTP response.
            message = String.format("%s\n%s", translation, getStackTrace(cause));
        }

        return Response.serverError()
            .entity(message)
            .type(mediaType)
            .build();
    }
}
