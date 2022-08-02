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

import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeneralMailScriptService}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
public class GeneralMailScriptServiceTest
{
    @InjectMockComponents
    private GeneralMailScriptService scriptService;

    @MockComponent
    private EmailAddressObfuscator obfuscator;

    @MockComponent
    private GeneralMailConfiguration configuration;

    @Test
    void shouldObfuscateEmailAddresses()
    {
        when(this.configuration.shouldObfuscate()).thenReturn(true);
        assertTrue(this.scriptService.shouldObfuscate());
    }

    @Test
    void obfuscate() throws Exception
    {
        InternetAddress address = InternetAddress.parse("john@doe.com")[0];
        when(this.obfuscator.obfuscate(address)).thenReturn("obfuscatedemail");
        assertEquals("obfuscatedemail", this.scriptService.obfuscate(address));
    }
}
