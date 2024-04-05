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
package org.xwiki.captcha;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.script.ScriptContextManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Abstract {@link Captcha} implementation.
 *
 * @version $Id$
 * @since 10.8RC1
 */
public abstract class AbstractCaptcha implements Captcha
{
    private static final String CAPTCHA_PARAMETERS_BINDING = "captchaParameters";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * @return the reference of the CAPTCHA displayer document
     */
    protected abstract LocalDocumentReference getDisplayerDocumentReference();

    /**
     * @return the reference of the CAPTCHA configuration document
     */
    protected abstract LocalDocumentReference getConfigurationDocumentReference();

    /**
     * @return the reference of the CAPTCHA configuration class document
     */
    protected abstract LocalDocumentReference getConfigurationClassReference();

    /**
     * @return the default CAPTCHA parameter values to be used in the absence of the configured ones
     */
    protected abstract Map<String, Object> getDefaultParameters();

    /**
     * Actual implementation validation.
     *
     * @param captchaParameters
     * @return {@code true} if the CAPTCHA answer in the context is valid
     * @throws Exception in case of problems
     */
    protected abstract boolean validate(Map<String, Object> captchaParameters) throws Exception;

    /**
     * @return the logger
     */
    protected Logger getLogger()
    {
        return logger;
    }

    /**
     * @return the scriptContextManager
     */
    protected ScriptContextManager getScriptContextManager()
    {
        return scriptContextManager;
    }

    @Override
    public String display() throws CaptchaException
    {
        // No parameter overrides, just use the default ones from the configuration.
        return display(null);
    }

    @Override
    public String display(Map<String, Object> parameters) throws CaptchaException
    {
        String result = null;
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();

            // Pass the parameters to the ScriptContext and display the CAPTCHA challenge.
            Map<String, Object> captchaParameters = getMergedParameters(parameters);
            scriptContextManager.getCurrentScriptContext().setAttribute(CAPTCHA_PARAMETERS_BINDING, captchaParameters,
                ScriptContext.ENGINE_SCOPE);

            // Use the Displayer document to render the CAPTCHA in the context of the current document.
            XWikiDocument displayerDocument = xwiki.getDocument(getDisplayerDocumentReference(), context);
            result = displayerDocument.getRenderedContent(context);
        } catch (Exception e) {
            throw new CaptchaException("Failed to display CAPTCHA", e);
        }

        return result;
    }

    @Override
    public boolean isValid() throws CaptchaException
    {
        return isValid(null);
    }

    @Override
    public boolean isValid(Map<String, Object> parameters) throws CaptchaException
    {
        Boolean result = false;

        try {
            Map<String, Object> captchaParameters = getMergedParameters(parameters);

            // Pass the validation logic to the implementation.
            result = validate(captchaParameters);
        } catch (Exception e) {
            throw new CaptchaException("Failed to validate CAPTCHA answer", e);
        }

        return result;
    }

    protected Map<String, Object> getMergedParameters(Map<String, Object> parameters)
    {
        // Merge the provided parameters, overriding default parameters.
        Map<String, Object> captchaParameters = getConfiguredParameters();
        if (parameters != null && parameters.size() > 0) {
            captchaParameters.putAll(parameters);
        }
        return captchaParameters;
    }

    protected Map<String, Object> getConfiguredParameters()
    {
        XWikiContext context = getContext();
        XWiki xwiki = context.getWiki();

        // Initialize with default values.
        Map<String, Object> parameters = new HashMap<>();
        parameters.putAll(getDefaultParameters());

        try {
            // Try to read actual values from the configuration document, overriding the default values.
            XWikiDocument configurationDoc = xwiki.getDocument(getConfigurationDocumentReference(), context);
            BaseObject configurationObj = configurationDoc.getXObject(getConfigurationClassReference());
            for (Object prop : configurationObj.getProperties()) {
                BaseProperty<?> property = (BaseProperty<?>) prop;
                Object value = property.getValue();
                if (value != null && !(value instanceof String stringValue && StringUtils.isEmpty(stringValue))) {
                    // Count only non-null values and non-empty strings.
                    parameters.put(property.getName(), value);
                }
            }
        } catch (Exception e) {
            getLogger().warn("Failed to read the configuration document [{}]. Using default values",
                getConfigurationDocumentReference(), e);
        }

        return parameters;
    }

    /**
     * @return the XWiki Context
     */
    protected XWikiContext getContext()
    {
        XWikiContext context = contextProvider.get();
        return context;
    }
}
