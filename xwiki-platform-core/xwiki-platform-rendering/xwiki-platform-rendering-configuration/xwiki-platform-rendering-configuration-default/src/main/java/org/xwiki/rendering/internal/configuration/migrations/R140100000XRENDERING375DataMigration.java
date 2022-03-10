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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Disable the new XHTML 5 and HTML 5.0 syntax on upgrade to 14.1 such that they are disabled by default.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Component
@Named("R140100000XRENDERING375DataMigration")
@Singleton
public class R140100000XRENDERING375DataMigration extends AbstractHibernateDataMigration
{
    private static final String DISABLED_SYNTAXES_PROPERTY = "disabledSyntaxes";

    @Inject
    @Named("rendering")
    private ConfigurationSource renderingConfiguration;

    @Override
    public String getDescription()
    {
        return "Disable the new XHTML 5 and HTML 5.0 syntax such that they are disabled by default.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140100000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException
    {
        List<String> disabledSyntaxesAsStrings = this.renderingConfiguration.getProperty(DISABLED_SYNTAXES_PROPERTY);
        // If nothing is set, the default of disabling everything apart from the default syntax is sufficient.
        if (disabledSyntaxesAsStrings != null && !disabledSyntaxesAsStrings.isEmpty()) {
            List<String> extendedDisabledSyntaxes = new ArrayList<>(disabledSyntaxesAsStrings);
            for (Syntax syntaxToDisable : List.of(Syntax.HTML_5_0, Syntax.XHTML_5)) {
                String idString = syntaxToDisable.toIdString();
                if (!extendedDisabledSyntaxes.contains(idString)) {
                    extendedDisabledSyntaxes.add(idString);
                }
            }

            if (!extendedDisabledSyntaxes.equals(disabledSyntaxesAsStrings)) {
                try {
                    this.renderingConfiguration.setProperties(
                        Map.of(DISABLED_SYNTAXES_PROPERTY, extendedDisabledSyntaxes));
                } catch (ConfigurationSaveException e) {
                    throw new DataMigrationException("Error disabling HTML 5.0 and XHTML 5", e);
                }
            }
        }
    }
}
