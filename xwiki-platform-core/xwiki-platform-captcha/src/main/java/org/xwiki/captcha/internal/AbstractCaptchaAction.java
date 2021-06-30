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

import org.xwiki.captcha.CaptchaVerifier;

import com.octo.captcha.service.CaptchaService;
import com.opensymphony.xwork2.ActionSupport;

/**
 * The AbstractCaptchaAction handles things which are needed by all types of captchas.
 * 
 * @param <T> The type of captcha service for the implementation.
 * @version $Id$
 * @since 2.2M2
 */
public abstract class AbstractCaptchaAction<T extends CaptchaService> extends ActionSupport
    implements CaptchaVerifier
{
    @Override
    public boolean isAnswerCorrect(String userId, String answer)
    {
        return getCaptchaService().validateResponseForID(userId, answer);
    }

    @Override
    public String getUserId(HttpServletRequest request)
    {
        return request.getSession().getId();
    }

    /** @return The CaptchaService which supplies the captchas */
    abstract T getCaptchaService();
}
