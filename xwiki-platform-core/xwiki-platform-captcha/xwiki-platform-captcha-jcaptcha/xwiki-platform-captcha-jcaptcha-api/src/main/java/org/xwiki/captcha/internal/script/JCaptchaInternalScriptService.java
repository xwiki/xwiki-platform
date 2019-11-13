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
package org.xwiki.captcha.internal.script;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.captcha.internal.JCaptchaResourceReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.url.ExtendedURL;

/**
 * A dedicated script service to manipulate {@link JCaptchaResourceReference}.
 *
 * @since 11.10RC1
 * @version $Id$
 */
@Component
@Named("jcaptcha")
@Singleton
public class JCaptchaInternalScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> defaultResourceReferenceSerializer;

    /**
     * Build an URL for the specific JCaptcha type and engine.
     * @param type the type of JCaptcha (e.g. audio, image, text)
     * @param engine the engine to use to create the captcha
     * @param params some other parameters to put in the URL
     * @return the URL to display the captcha.
     */
    public String getURL(String type, String engine, Map<String, Object> params)
    {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(engine)) {
            return null;
        }
        // Construct a JCaptchaResourceReference so that we can serialize it
        JCaptchaResourceReference resourceReference = new JCaptchaResourceReference(type, engine);
        for (Map.Entry<String, Object> parameterEntry : params.entrySet()) {
            resourceReference.addParameter(parameterEntry.getKey(), parameterEntry.getValue());
        }

        ExtendedURL extendedURL;
        try {
            extendedURL = this.defaultResourceReferenceSerializer.serialize(resourceReference);
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            this.logger.warn("Error while serializing WebJar URL for type [{}], engine = [{}]. Root cause = [{}]",
                type, engine, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }

        return extendedURL.serialize();
    }
}
