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
package org.xwiki.mailSender;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mailSender.MailSender;
import org.xwiki.mailSender.internal.DefaultMailSenderUtils;
import org.xwiki.mailSender.internal.Mail;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.jvnet.mock_javamail.Mailbox;

import static org.mockito.Mockito.*;

/**
 * Tests for the {@link MailSender} component.
 */
@ComponentList({DefaultMailSenderUtils.class})
public class MailSenderUtilsTest 
{
    
    @Rule
    public final MockitoComponentMockingRule<MailSenderUtils> mocker = new MockitoComponentMockingRule<MailSenderUtils>(
        DefaultMailSenderUtils.class);
    
    @Before
    public void configure()
    {
    }
}
