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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wysiwyg.converter.HTMLConverter;
import org.xwiki.wysiwyg.converter.JakartaRequestParameterConversionResult;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;

/**
 * A {@link RequestParameterConverter} converts the parameter values, where needed, from HTML to a specified wiki
 * syntax.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Singleton
@Named("html")
public class HTMLRequestParameterConverter extends AbstractRequestParameterConverter
{
    @Inject
    private HTMLConverter htmlConverter;

    /**
     * @return the name of the request parameter whose multiple values indicate the request parameters that require HTML
     *         conversion; for instance, if this parameter's value is {@code [description, content]} then the request
     *         has two parameters, {@code description} and {@code content}, requiring HTML conversion; the syntax these
     *         parameters must be converted to is found also on the request, under {@code description_syntax} and
     *         {@code content_syntax} parameters
     */
    @Override
    protected String getConverterParameterName()
    {
        return "RequiresHTMLConversion";
    }

    protected void convert(String[] parametersRequiringConversion,
        JakartaRequestParameterConversionResult conversionResult)
    {
        MutableJakartaServletRequest request = conversionResult.getRequest();
        for (String parameterName : parametersRequiringConversion) {
            String html = request.getParameter(parameterName);
            // Remove the syntax parameter from the request to avoid interference with further request processing.
            String syntax = request.removeParameter(parameterName + "_syntax");
            if (html == null || syntax == null) {
                continue;
            }
            try {
                request.setParameter(parameterName, this.htmlConverter.fromHTML(html, syntax));
            } catch (Exception e) {
                this.logger.error(e.getLocalizedMessage(), e);
                conversionResult.getErrors().put(parameterName, e);
            }
            // If the conversion fails the output contains the value before the conversion.
            conversionResult.getOutput().put(parameterName, request.getParameter(parameterName));
        }
    }
}
