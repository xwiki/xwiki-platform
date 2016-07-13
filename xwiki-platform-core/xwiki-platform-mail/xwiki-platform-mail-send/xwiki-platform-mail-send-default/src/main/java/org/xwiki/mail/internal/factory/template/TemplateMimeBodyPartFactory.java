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
package org.xwiki.mail.internal.factory.template;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Creates an Body Part from a Document Reference pointing to a Document containing an XWiki.Mail XObject (the first one
 * found is used).
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("xwiki/template")
@Singleton
public class TemplateMimeBodyPartFactory extends AbstractTemplateMimeBodyPartFactory
{
    @Inject
    private MailTemplateManager mailTemplateManager;

    @Override
    protected MailTemplateManager getTemplateManager()
    {
        return this.mailTemplateManager;
    }
}
