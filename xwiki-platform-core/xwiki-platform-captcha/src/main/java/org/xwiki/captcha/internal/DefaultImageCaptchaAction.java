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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;

/**
 * The DefaultImageCaptchaAction creates a simple generic jpeg image captcha.
 * 
 * @version $Id$
 * @since 2.2M2
 */
@Component
@Named("image")
@Singleton
public class DefaultImageCaptchaAction extends AbstractImageCaptchaAction
{
    /** The service which provides the captcha, must be static because struts instantiates per request. */
    private static final ImageCaptchaService SERVICE = new DefaultManageableImageCaptchaService();

    @Override
    ImageCaptchaService getCaptchaService()
    {
        return SERVICE;
    }
}
