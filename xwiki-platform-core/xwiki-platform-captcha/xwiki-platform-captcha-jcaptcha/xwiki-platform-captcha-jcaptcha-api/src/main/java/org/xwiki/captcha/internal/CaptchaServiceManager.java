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

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.captcha.CaptchaException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.octo.captcha.engine.CaptchaEngine;
import com.octo.captcha.service.CaptchaService;
import com.octo.captcha.service.captchastore.CaptchaStore;
import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore;
import com.octo.captcha.service.multitype.GenericManageableCaptchaService;

/**
 * Always uses a fixed/common {@link CaptchaStore} and the specified parameters in order to obtain a
 * {@link CaptchaService} instance.
 *
 * @version $Id$
 * @since 10.8RC1
 */
@Component(roles = CaptchaServiceManager.class)
@Singleton
public class CaptchaServiceManager implements Initializable
{
    private CaptchaStore captchaStore;

    @Override
    public void initialize() throws InitializationException
    {
        // Use an in-memory captcha store.
        captchaStore = new FastHashMapCaptchaStore();
    }

    /**
     * @return the common {@link CaptchaStore} used by all the instantiated {@link CaptchaService} instance
     */
    public CaptchaStore getCaptchaStore()
    {
        return captchaStore;
    }

    /**
     * @param parameters CAPTCHA parameters
     * @return the JCaptcha CaptchaService initialized with the requested engine
     * @throws CaptchaException if the requested engine does not exist
     */
    public CaptchaService getCaptchaService(Map<String, Object> parameters) throws CaptchaException
    {
        CaptchaService result = null;

        String engine = (String) parameters.get("engine");

        CaptchaEngine captchaEngine = null;

        try {
            Class<CaptchaEngine> engineClass = (Class<CaptchaEngine>) Class.forName(engine);
            captchaEngine = engineClass.getConstructor(null).newInstance(null);
        } catch (Exception e) {
            throw new CaptchaException(String.format("Invalid engine [%s]", engine), e);
        }

        result = new GenericManageableCaptchaService(captchaStore, captchaEngine, 180, 100000, 75000);

        return result;
    }
}
