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
package org.xwiki.test.integration;

import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches an XWiki URL to tell if the XWiki instance is started or not.
 *
 * @version $Id$
 * @since 10.9
 */
public class XWikiWatchdog
{
    private static final boolean DEBUG = System.getProperty("debug", "false").equalsIgnoreCase("true");

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiWatchdog.class);

    /**
     * @param url the URL to watch
     * @param timeout the timeout after which we consider that the XWiki instance is not running
     * @return a response object containing various information about the XWiki URL (response code, timeout, etc)
     *         allowing the caller to know if the XWiki instance is up or not
     * @throws Exception if a thread has interrupted the thread in which this code is executing
     */
    public WatchdogResponse isXWikiStarted(String url, long timeout) throws Exception
    {
        WatchdogResponse response = new WatchdogResponse();
        response.timedOut = false;
        response.responseCode = -1;
        response.responseBody = "";

        HttpClientBuilder builder = HttpClients
            .custom()
            // Don't retry automatically since we're doing that in the algorithm below
            .useSystemProperties().setRetryStrategy(
                new DefaultHttpRequestRetryStrategy(0, TimeValue.ZERO_MILLISECONDS));

        try (CloseableHttpClient httpclient = builder.build()) {
            boolean connected = false;
            long startTime = System.currentTimeMillis();
            while (!connected && !response.timedOut) {
                HttpGet httpGet = new HttpGet(url);

                // Set a socket timeout to ensure the server has no chance of not answering to our request...
                RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(10000, TimeUnit.MILLISECONDS))
                    .setResponseTimeout(Timeout.of(10000, TimeUnit.MILLISECONDS))
                    .build();
                HttpClientContext httpContext = HttpClientContext.create();
                httpContext.setRequestConfig(requestConfig);

                try (CloseableHttpResponse httpResponse = httpclient.execute(httpGet, httpContext)) {
                    HttpEntity entity = httpResponse.getEntity();
                    response.responseBody = IOUtils.toString(entity.getContent(), "UTF-8");
                    if (DEBUG) {
                        LOGGER.info("Result of pinging [{}] = [{}], Message = [{}]", url, response.responseCode,
                            response.responseBody);
                    }

                    // check the http response code is either not an error, either "unauthorized"
                    // (which is the case for products that deny view for guest, for example).
                    connected = (response.responseCode < 400 || response.responseCode == 401);
                } catch (Exception e) {
                    // Do nothing as it simply means the server is not ready yet...
                }
                response.timedOut = (System.currentTimeMillis() - startTime > timeout * 1000L);
                Thread.sleep(500L);
            }
        }

        if (response.timedOut) {
            LOGGER.info("No server is responding on [{}] after [{}] seconds", url, timeout);
        }

        return response;
    }
}
