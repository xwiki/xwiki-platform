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
package com.xpn.xwiki.internal.mandatory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.XWikiUsers document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.GlobalRedirect")
@Singleton
public class GlobalRedirectDocumentInitializer extends AbstractMandatoryClassInitializer
{
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configuration;

    /**
     * Default constructor.
     */
    public GlobalRedirectDocumentInitializer()
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, "GlobalRedirect"));
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        return true;
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("pattern", "Pattern", 30);
        xclass.addTextField("destination", "Destination", 30);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        // Only on main wiki and only when enabled in the configuration
        if (this.configuration.getProperty("xwiki.preferences.redirect", false)) {
            return false;
        }

        return super.updateDocument(document);
    }
}
