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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;

import com.octo.captcha.module.web.sound.SoundToWavHelper;
import com.octo.captcha.service.sound.SoundCaptchaService;

/**
 * Extensions of AbstractSoundCaptchaAction create wav audio captchas.
 *
 * @version $Id$
 * @since 2.2M2
 */
public abstract class AbstractSoundCaptchaAction extends AbstractCaptchaAction<SoundCaptchaService>
{
    @Override
    public String execute() throws Exception
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpServletResponse response = ServletActionContext.getResponse();

        SoundToWavHelper.flushNewCaptchaToResponse(request, response, null, getCaptchaService(), getUserId(request),
            request.getLocale());

        return null;
    }
}
