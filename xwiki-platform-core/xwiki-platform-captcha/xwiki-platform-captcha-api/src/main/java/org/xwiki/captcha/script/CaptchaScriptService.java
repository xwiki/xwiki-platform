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
package org.xwiki.captcha.script;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.internal.safe.ScriptSafeProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Exposes {@link Captcha}s to scripts.
 *
 * @version $Id$
 * @since 10.8RC1
 */
@Unstable
@Component
@Named("captcha")
@Singleton
public class CaptchaScriptService implements ScriptService
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private CaptchaConfiguration configuration;

    @Inject
    private ScriptSafeProvider<Captcha> safeProvider;

    @Inject
    private Logger logger;

    /**
     * @return the list of available CAPTCHA implementation names
     */
    public List<String> getCaptchaNames()
    {
        List<String> names = new ArrayList<>();
        try {
            names = new ArrayList<>(componentManager.getInstanceMap(Captcha.class).keySet());
        } catch (Exception e) {
            logger.error("Failed to get list of captcha names", e);
        }

        return names;
    }

    /**
     * @param captchaName the name of the CAPTCHA implementation to get
     * @return the CAPTCHA implementation
     */
    public Captcha get(String captchaName)
    {
        try {
            return safeProvider.get(getCaptcha(captchaName));
        } catch (ComponentLookupException e) {
            // Nothing special, just a bad request; return null.
            logger.warn("No CAPTCHA implementation named [{}] was found", captchaName);
        } catch (Exception e) {
            // Log the actual error.
            logger.error("Failed to get CAPTCHA implementation with name [{}]", captchaName, e);
        }

        return null;
    }

    /**
     * @return the name of the configured CAPTCHA implementation to use by default
     */
    public String getDefaultCaptchaName()
    {
        return configuration.getDefaultName();
    }

    /**
     * @return the configured CAPTCHA implementation to use by default
     */
    public Captcha getDefault()
    {
        return get(getDefaultCaptchaName());
    }

    private Captcha getCaptcha(String captchaName) throws ComponentLookupException
    {
        return componentManager.getInstance(Captcha.class, captchaName);
    }
}
