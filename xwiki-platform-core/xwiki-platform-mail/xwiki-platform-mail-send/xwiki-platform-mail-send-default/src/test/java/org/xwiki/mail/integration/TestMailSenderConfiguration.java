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
package org.xwiki.mail.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.xwiki.mail.internal.configuration.DefaultMailSenderConfiguration;

/**
 * Makes {@link DefaultMailSenderConfiguration} more easily testable.
 *
 * @version $Id$
 */
public class TestMailSenderConfiguration extends DefaultMailSenderConfiguration
{
    private int port;

    private String username;

    private String password;

    private Properties additionalProperties;

    private List<String> bccAddresses = new ArrayList<>();

    public TestMailSenderConfiguration(int port, String username, String password, Properties additionalProperties)
    {
        this.port = port;
        this.username = username;
        this.password = password;
        this.additionalProperties = additionalProperties;
    }

    @Override
    public String getHost()
    {
        return "localhost";
    }

    @Override
    public int getPort()
    {
        return this.port;
    }

    @Override
    public String getFromAddress()
    {
        return "mary@doe.com";
    }

    @Override
    public List<String> getBCCAddresses()
    {
        return this.bccAddresses;
    }

    @Override
    public String getUsername()
    {
        return this.username;
    }

    @Override
    public String getPassword()
    {
        return this.password;
    }

    @Override
    public Properties getAdditionalProperties()
    {
        return this.additionalProperties;
    }

    @Override
    public String getScriptServicePermissionCheckerHint()
    {
        return "test";
    }

    @Override
    public long getSendWaitTime()
    {
        return 0;
    }

    @Override
    public int getPrepareQueueCapacity()
    {
        return 10;
    }

    @Override
    public int getSendQueueCapacity()
    {
        return 10;
    }

    public void setBCCAddresses(List<String> addresses)
    {
        this.bccAddresses = addresses;
    }
}
