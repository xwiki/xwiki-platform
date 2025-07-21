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
package org.xwiki.index.tree.internal.nestedpages.query;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Named;

import org.apache.commons.io.IOUtils;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Abstract base class for query registration handler tests.
 *
 * @version $Id$
 */
public abstract class AbstractQueryRegistrationHandlerTest
{
    @MockComponent
    @Named("xwikiproperties")
    protected ConfigurationSource configuration;

    @MockComponent
    protected HibernateSessionFactory hibernateSessionFactory;

    @Mock
    protected Configuration hibernateConfiguration;

    @BeforeEach
    void setUp()
    {
        when(this.hibernateSessionFactory.getConfiguration()).thenReturn(this.hibernateConfiguration);
    }

    protected StringBuilder mockConfigurationSetting(String collation)
    {
        when(this.configuration.getProperty("index.sortCollation", ""))
            .thenReturn(collation);

        StringBuilder configurationString = new StringBuilder();
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgument(0);
            configurationString.append(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            return null;
        }).when(this.hibernateConfiguration).addInputStream(any());
        return configurationString;
    }
}
