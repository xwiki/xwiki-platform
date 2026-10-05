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
package org.xwiki.wysiwyg.internal.converter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.wysiwyg.converter.HTMLConverter;
import org.xwiki.wysiwyg.converter.RequestParameterConversionResult;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;
import org.xwiki.wysiwyg.filter.MutableServletRequest;
import org.xwiki.wysiwyg.filter.MutableServletRequestFactory;

/**
 * Default implementation of {@link RequestParameterConverter} that handles HTML conversion of parameters needing such
 * conversion.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@Component
@Singleton
public class DefaultRequestParameterConverter implements RequestParameterConverter
{
    /**
     * The name of the request parameter whose multiple values indicate the request parameters that require HTML
     * conversion. For instance, if this parameter's value is {@code [description, content]} then the request has two
     * parameters, {@code description} and {@code content}, requiring HTML conversion. The syntax these parameters must
     * be converted to is found also on the request, under {@code description_syntax} and {@code content_syntax}
     * parameters.
     */
    private static final String REQUIRES_HTML_CONVERSION = "RequiresHTMLConversion";

    @Inject
    private MutableServletRequestFactory mutableServletRequestFactory;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private URLSecurityManager urlSecurityManager;

    @Inject
    private Logger logger;

    @Override
    public Optional<ServletRequest> convert(ServletRequest request, ServletResponse response) throws IOException
    {
        RequestParameterConversionResult conversionResult = this.convert(request);
        Optional<ServletRequest> result;
        if (conversionResult.getErrors().isEmpty()) {
            result = Optional.of(conversionResult.getRequest());
        } else {
            result = Optional.empty();
            this.handleConversionErrors(conversionResult, response);
        }
        return result;
    }

    @Override
    public RequestParameterConversionResult convert(ServletRequest request)
    {
        MutableServletRequest mutableServletRequest = this.mutableServletRequestFactory.newInstance(request);
        RequestParameterConversionResult result = new RequestParameterConversionResult(mutableServletRequest);
        // Take the list of request parameters that require HTML conversion.
        String[] parametersRequiringHTMLConversion = request.getParameterValues(REQUIRES_HTML_CONVERSION);
        if (parametersRequiringHTMLConversion != null) {
            // Remove the list of request parameters that require HTML conversion to avoid recurrency.
            result.getRequest().removeParameter(REQUIRES_HTML_CONVERSION);
            convertHTML(parametersRequiringHTMLConversion, result);
        }
        return result;
    }

    private void convertHTML(String[] parametersRequiringHTMLConversion,
        RequestParameterConversionResult conversionResult)
    {
        MutableServletRequest request = conversionResult.getRequest();
        for (String parameterName : parametersRequiringHTMLConversion) {
            String html = request.getParameter(parameterName);
            // Remove the syntax parameter from the request to avoid interference with further request processing.
            String syntax = request.removeParameter(parameterName + "_syntax");
            if (html == null || syntax == null) {
                continue;
            }
            try {
                request.setParameter(parameterName, this.htmlConverter.fromHTML(html, syntax));
            } catch (Exception e) {
                this.logger.warn("Failed to convert the [{}] request parameter. Root cause is [{}].", parameterName,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace for the conversion failure:", e);
                conversionResult.getErrors().put(parameterName, e);
            }
            // If the conversion fails the output contains the value before the conversion.
            conversionResult.getOutput().put(parameterName, request.getParameter(parameterName));
        }
    }

    private void handleConversionErrors(RequestParameterConversionResult conversionResult, ServletResponse res)
        throws IOException
    {
        MutableServletRequest mutableRequest = conversionResult.getRequest();
        ServletRequest originalRequest = mutableRequest.getRequest();
        if (originalRequest instanceof HttpServletRequest httpServletRequest
            && "XMLHttpRequest".equals((httpServletRequest).getHeader("X-Requested-With"))) {
            // If this is an AJAX request then we should simply send back the error.
            StringBuilder errorMessage = new StringBuilder();
            // Aggregate all error messages (for all fields that have conversion errors).
            for (Map.Entry<String, Throwable> entry : conversionResult.getErrors().entrySet()) {
                errorMessage.append(entry.getKey()).append(": ");
                errorMessage.append(entry.getValue().getLocalizedMessage()).append('\n');
            }
            ((HttpServletResponse) res).sendError(400, errorMessage.substring(0, errorMessage.length() - 1));
            return;
        }
        // Otherwise, if this is a normal request, redirect to the error page specified on the request.
        String unsafeURL = mutableRequest.getParameter("xerror");
        if (unsafeURL == null) {
            // Redirect to the referrer page.
            unsafeURL = mutableRequest.getReferer();
        }
        if (originalRequest instanceof HttpServletRequest httpRequest) {
            try {
                URI safeURI = this.urlSecurityManager.parseToSafeURI(unsafeURL, httpRequest.getServerName());
                mutableRequest.sendRedirect(res, safeURI.toString());
            } catch (URISyntaxException | SecurityException e) {
                this.logger.warn(
                    "Possible phishing attack, attempting to redirect to [{}], this request has been blocked. "
                        + "If the request was legitimate, please check the URL security configuration. You "
                        + "might need to add the domain related to this request in the list of trusted domains in "
                        + "the configuration: it can be configured in xwiki.properties in url.trustedDomains.",
                    unsafeURL);
                this.logger.debug("Original error preventing the redirect: ", e);
                ((HttpServletResponse) res).sendError(400, "The error redirect URI isn't considered safe.");
            }
        }
    }
}
