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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.wysiwyg.converter.JakartaRequestParameterConversionResult;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;
import org.xwiki.wysiwyg.filter.MutableServletRequestFactory;

/**
 * Abstract base class for {@link RequestParameterConverter} implementations.
 * 
 * @version $Id$
 * @since 17.4.0RC1
 */
public abstract class AbstractRequestParameterConverter implements RequestParameterConverter
{
    /**
     * The name of the session attribute holding the conversion output. The conversion output is stored in a {@link Map}
     * of {@link Map}s. The first key identifies the request and the second key is the name of the request parameter
     * that required conversion.
     */
    private static final String CONVERSION_OUTPUT = "com.xpn.xwiki.wysiwyg.server.converter.output";

    /**
     * The name of the session attribute holding the conversion exceptions. The conversion exceptions are stored in a
     * {@link Map} of {@link Map}s. The first key identifies the request and the second key is the name of the request
     * parameter that required conversion.
     */
    private static final String CONVERSION_ERRORS = "com.xpn.xwiki.wysiwyg.server.converter.errors";

    @Inject
    protected Logger logger;

    @Inject
    private MutableServletRequestFactory mutableServletRequestFactory;

    @Inject
    private URLSecurityManager urlSecurityManager;

    @Override
    public Optional<ServletRequest> convert(ServletRequest request, ServletResponse response) throws IOException
    {
        JakartaRequestParameterConversionResult conversionResult = convert(request);
        Optional<ServletRequest> result;
        if (conversionResult.getErrors().isEmpty()) {
            result = Optional.of(conversionResult.getRequest());
        } else {
            result = Optional.empty();
            handleConversionErrors(conversionResult, response);
        }

        return result;
    }

    @Override
    public JakartaRequestParameterConversionResult convert(ServletRequest request)
    {
        MutableJakartaServletRequest mutableServletRequest =
            (request instanceof MutableJakartaServletRequest mutableJakartaServletRequest)
                ? mutableJakartaServletRequest : this.mutableServletRequestFactory.newInstance(request);
        JakartaRequestParameterConversionResult result =
            new JakartaRequestParameterConversionResult(mutableServletRequest);

        // Take the list of request parameters that require conversion.
        String[] parametersRequiringConversion = request.getParameterValues(getConverterParameterName());
        if (parametersRequiringConversion != null) {
            // Remove the list of request parameters that require conversion to avoid recurrency.
            result.getRequest().removeParameter(getConverterParameterName());
            convert(parametersRequiringConversion, result);
        }

        return result;
    }

    /**
     * @return the name of the request parameter whose multiple values indicate the request parameters that require
     *         conversion; for instance, if this parameter's value is {@code [description, content]} then the request
     *         has two parameters, {@code description} and {@code content}, requiring conversion; the input and output
     *         syntax used for the conversion is determined by the concrete implementation, usually based on other
     *         request parameters
     */
    protected abstract String getConverterParameterName();

    protected abstract void convert(String[] parametersRequiringConversion,
        JakartaRequestParameterConversionResult conversionResult);

    private void handleConversionErrors(JakartaRequestParameterConversionResult conversionResult, ServletResponse res)
        throws IOException
    {
        MutableJakartaServletRequest mutableRequest = conversionResult.getRequest();
        ServletRequest originalRequest = mutableRequest.getRequest();
        if (originalRequest instanceof HttpServletRequest httpRequest
            && "XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"))) {
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
        // Otherwise, if this is a normal request, we have to redirect the request back and provide a key to
        // access the exception and the value before the conversion from the session.
        // Redirect to the error page specified on the request.
        String redirectURL = mutableRequest.getParameter("xerror");
        if (redirectURL == null) {
            // Redirect to the referrer page.
            redirectURL = mutableRequest.getReferer();
        }
        // Extract the query string.
        String queryString = StringUtils.substringAfterLast(redirectURL, String.valueOf('?'));
        // Remove the query string.
        redirectURL = StringUtils.substringBeforeLast(redirectURL, String.valueOf('?'));
        // Remove the previous key from the query string. We have to do this since this might not be the first
        // time the conversion fails for this redirect URL.
        queryString = queryString.replaceAll("key=.*&?", "");
        if (!queryString.isEmpty() && !queryString.endsWith(String.valueOf('&'))) {
            queryString += '&';
        }
        // Save the output and the caught exceptions on the session.
        queryString += "key=" + save(conversionResult);
        String unsafeURL = redirectURL + '?' + queryString;
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

    /**
     * Saves on the session the conversion output and the caught conversion exceptions, after a conversion failure.
     *
     * @param conversionResult the result of the conversion
     * @return a key that can be used along with the name of the request parameters that required conversion to extract
     *         the conversion output and the conversion exceptions from the {@link #CONVERSION_OUTPUT} and
     *         {@value #CONVERSION_ERRORS} session attributes
     */
    @SuppressWarnings("unchecked")
    private String save(JakartaRequestParameterConversionResult conversionResult)
    {
        // Generate a random key to identify the request.
        String key = RandomStringUtils.secure().nextAlphanumeric(4);
        MutableJakartaServletRequest request = conversionResult.getRequest();

        // Save the output on the session.
        Map<String, Map<String, String>> conversionOutput =
            (Map<String, Map<String, String>>) request.getSessionAttribute(CONVERSION_OUTPUT);
        if (conversionOutput == null) {
            conversionOutput = new HashMap<>();
            request.setSessionAttribute(CONVERSION_OUTPUT, conversionOutput);
        }
        conversionOutput.put(key, conversionResult.getOutput());

        // Save the errors on the session.
        Map<String, Map<String, Throwable>> conversionErrors =
            (Map<String, Map<String, Throwable>>) request.getSessionAttribute(CONVERSION_ERRORS);
        if (conversionErrors == null) {
            conversionErrors = new HashMap<>();
            request.setSessionAttribute(CONVERSION_ERRORS, conversionErrors);
        }
        conversionErrors.put(key, conversionResult.getErrors());

        return key;
    }
}
