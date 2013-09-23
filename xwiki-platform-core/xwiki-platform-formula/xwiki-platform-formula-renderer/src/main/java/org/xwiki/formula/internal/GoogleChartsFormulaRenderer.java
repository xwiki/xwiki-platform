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
package org.xwiki.formula.internal;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.formula.AbstractFormulaRenderer;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.ImageData;

/**
 * Implementation of the {@link FormulaRenderer} component, which uses the <a
 * href="http://code.google.com/apis/chart/">Google Chart APIs</a> as a remote service for rendering mathematical
 * formulae. The quality of the results is between those obtained from the native TeX system and those from SnuggleTeX,
 * with the advantage that it doesn't require the presence of a local TeX installation, but with the disadvantages that
 * it requires fast, continuous access to an external server, for high traffic you must notify Google of your usage,
 * only block-level equations are produced, there's no support (yet?) for different font sizes and image formats, and
 * the results are not 100% in accordance with the TeX syntax (for example it does not understand \root, the content
 * between \left and \right is lowered). This service is still in an early stage, so it might improve in the future.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Component("googlecharts")
public final class GoogleChartsFormulaRenderer extends AbstractFormulaRenderer implements Initializable
{
    /** The base URL of the mathematical formula transformation service. */
    private static final String SERVICE_BASE_URL =
        "http://chart.apis.google.com/chart?cht=tx&chf=bg,s,FFFFFF00&chco=000000&chl=";

    /** Handles requests to the mathematical formula transformation service. */
    private final HttpClient client = new HttpClient();

    @Override
    public void initialize() throws InitializationException
    {
        this.client.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageData renderImage(String formula, boolean inline, FormulaRenderer.FontSize size,
        FormulaRenderer.Type type) throws IllegalArgumentException, IOException
    {
        String encodedFormula = URLEncoder.encode(formula, "UTF-8");
        GetMethod method = new GetMethod(SERVICE_BASE_URL + encodedFormula);
        method.setRequestHeader("accept", type.getMimetype());
        method.setFollowRedirects(true);

        // Execute the GET method
        int statusCode = this.client.executeMethod(method);
        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_BAD_REQUEST) {
            byte[] b = method.getResponseBody();
            method.releaseConnection();
            return new ImageData(b, type);
        }

        throw new IOException("Can't load image from the Google server");
    }
}
