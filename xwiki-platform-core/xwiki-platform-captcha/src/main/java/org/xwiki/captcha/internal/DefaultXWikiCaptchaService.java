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
package org.xwiki.captcha.internal;

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.captcha.CaptchaVerifier;
import org.xwiki.captcha.CaptchaVerifierNotFoundException;
import org.xwiki.captcha.XWikiCaptchaService;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.annotation.Component;

/**
 * Provides access to the classes implementing Captcha.
 *
 * @version $Id$
 * @since 2.2M2
 */
@Component
@Singleton
public class DefaultXWikiCaptchaService implements XWikiCaptchaService
{
    /** A Map of all captchas by their names. */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public CaptchaVerifier getCaptchaVerifier(String captchaName) throws CaptchaVerifierNotFoundException
    {
        CaptchaVerifier captchaVerifier;
        try {
            captchaVerifier = componentManager.getInstance(CaptchaVerifier.class, captchaName);
        } catch (ComponentLookupException e) {
            throw new CaptchaVerifierNotFoundException("The CaptchaVerifier " + captchaName
                                                       + " could not be found, try listCaptchaNames()"
                                                       + " for a list of registered CaptchaVerifiers.");
        }
        return captchaVerifier;
    }

    @Override
    public List<String> listCaptchaNames()
    {
        List<String> captchaNames = new ArrayList<String>();
        try {
            captchaNames.addAll(componentManager.getInstanceMap(CaptchaVerifier.class).keySet());
        } catch (ComponentLookupException e) {
            this.logger.error("Couldn't get list of CaptchaVerifier names " + e.getMessage());
        }
        return captchaNames;
    }

    @Override
    @Deprecated
    public boolean isEnabled()
    {
        return true;
    }
}
