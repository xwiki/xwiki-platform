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
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Dedicated configuration source for jcaptcha.
 *
 * @version $Id$
 * @since 18.6.0RC1
 * @since 18.4.3
 * @since 17.10.10
 */
@Component
@Named("jcaptcha")
@Singleton
public class JCaptchaConfigurationSource extends AbstractDocumentConfigurationSource
{
    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(JCaptchaCaptcha.CONFIGURATION_DOCUMENT_REFERENCE, getCurrentWikiReference());
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return JCaptchaCaptcha.CONFIGURATION_CLASS_REFERENCE;
    }

    @Override
    protected String getCacheId()
    {
        return "configuration.document.jcaptcha";
    }
}
