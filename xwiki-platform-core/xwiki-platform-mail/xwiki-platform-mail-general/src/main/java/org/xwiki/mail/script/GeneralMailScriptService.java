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
package org.xwiki.mail.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.internet.InternetAddress;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.script.service.ScriptService;

/**
 * Access general mail APIs from scripts.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named("mail.general")
@Singleton
public class GeneralMailScriptService implements ScriptService
{
    @Inject
    private GeneralMailConfiguration configuration;

    @Inject
    private EmailAddressObfuscator obfuscator;

    /**
     * @return true when email addresses must be obfuscated and false otherwise. Defaults to false.
     */
    public boolean shouldObfuscate()
    {
        return this.configuration.shouldObfuscate();
    }

    /**
     * @param emailAddress the email address to obfuscate
     * @return the obfuscated email address (e.g. {@code j...@doe.com} for {@code john@doe.com})
     */
    public String obfuscate(InternetAddress emailAddress)
    {
        return this.obfuscator.obfuscate(emailAddress);
    }
}
