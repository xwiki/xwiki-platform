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
package org.xwiki.extension.security.internal.configuration;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration.REVIEWS_URL;
import static org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration.SCAN_DELAY;
import static org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration.SCAN_ENABLED;
import static org.xwiki.extension.security.internal.configuration.DefaultExtensionSecurityConfiguration.SCAN_URL;

/**
 * Initialize the XClass used to configure the extension security.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named("XWiki.Extension.Security.Code.ConfigClass")
public class ConfigClassMandatoryInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default constructor.
     */
    public ConfigClassMandatoryInitializer()
    {
        super(DocConfigurationSource.XCLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        super.createClass(xclass);

        xclass.addBooleanField(SCAN_ENABLED, "Scan Enabled", "checkbox", true);
        xclass.addNumberField(SCAN_DELAY, "Scan Delay", 30, "integer");
        xclass.addTextField(SCAN_URL, "Scan URL", 30);
        xclass.addTextField(REVIEWS_URL, "Reviews URL", 30);
    }
}
