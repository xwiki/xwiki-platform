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
package org.xwiki.rendering.internal.configuration.migrations;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link R140100000XRENDERING375DataMigration}.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@ComponentTest
class R140100000XRENDERING375DataMigrationTest
{
    private static final String DISABLED_SYNTAXES_PROPERTY = "disabledSyntaxes";

    @MockComponent
    @Named("rendering")
    private ConfigurationSource renderingConfiguration;

    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140100000XRENDERING375DataMigration r140100000XRENDERING375DataMigration;

    @Test
    void doNothingWhenEmpty() throws DataMigrationException
    {
        when(this.renderingConfiguration.getProperty(DISABLED_SYNTAXES_PROPERTY)).thenReturn(null);

        this.r140100000XRENDERING375DataMigration.migrate();

        verify(this.renderingConfiguration).getProperty(DISABLED_SYNTAXES_PROPERTY);
        verifyNoMoreInteractions(this.renderingConfiguration);
    }

    @Test
    void doNothingWhenAlreadyAdded() throws DataMigrationException
    {
        when(this.renderingConfiguration.getProperty(DISABLED_SYNTAXES_PROPERTY)).thenReturn(
            List.of(Syntax.XWIKI_2_0.toIdString(), Syntax.HTML_5_0.toIdString(), Syntax.XHTML_5.toIdString()));

        this.r140100000XRENDERING375DataMigration.migrate();

        verify(this.renderingConfiguration).getProperty(DISABLED_SYNTAXES_PROPERTY);
        verifyNoMoreInteractions(this.renderingConfiguration);
    }

    @Test
    void addXHTML5AndHTML5() throws DataMigrationException, ConfigurationSaveException
    {
        when(this.renderingConfiguration.getProperty(DISABLED_SYNTAXES_PROPERTY)).thenReturn(
            List.of(Syntax.XWIKI_2_0.toIdString()));

        this.r140100000XRENDERING375DataMigration.migrate();

        verify(this.renderingConfiguration).setProperties(Map.of(DISABLED_SYNTAXES_PROPERTY,
            List.of(Syntax.XWIKI_2_0.toIdString(), Syntax.HTML_5_0.toIdString(), Syntax.XHTML_5.toIdString())));
    }
}
