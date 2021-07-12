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
package org.xwiki.administration.internal.migration;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.xar.internal.event.WrappingExtensionEvent;
import org.xwiki.extension.xar.internal.event.XarExtensionInstalledEvent;
import org.xwiki.extension.xar.internal.event.XarExtensionUpgradedEvent;
import org.xwiki.extension.xar.internal.handler.UnsupportedNamespaceException;
import org.xwiki.extension.xar.internal.handler.XarHandlerUtils;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Listener that listens on installed or upgraded extensions to ensure to migration the configurable class they might
 * contain.
 *
 * @version $Id$
 * @since 13.6RC1
 */
@Component
@Named(ConfigurableClassMigrationXarExtensionListener.NAME)
@Singleton
public class ConfigurableClassMigrationXarExtensionListener extends AbstractEventListener
{
    static final String NAME = "ConfigurableClassMigrationXarExtensionListener";
    private static final List<Event> EVENT_LIST = Arrays.asList(
        new XarExtensionInstalledEvent(),
        new XarExtensionUpgradedEvent()
    );

    @Inject
    private ConfigurableClassScopeMigrator configurableClassScopeMigrator;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ConfigurableClassMigrationXarExtensionListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        WrappingExtensionEvent extensionEvent = (WrappingExtensionEvent) event;
        if (extensionEvent.hasNamespace()) {
            XWikiContext context = this.contextProvider.get();
            WikiReference currentWiki = context.getWikiReference();

            try {
                String wikiName = XarHandlerUtils.getWikiFromNamespace(extensionEvent.getNamespace());
                context.setWikiId(wikiName);
                this.configurableClassScopeMigrator.migrateAllConfigurableClass();
            } catch (QueryException e) {
                this.logger.warn("Error while performing query to migrate ConfigurableClass: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            } catch (XWikiException e) {
                this.logger.warn("Error while migrating a ConfigurableClass xobject: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            } catch (UnsupportedNamespaceException e) {
                this.logger.warn("Error while getting wiki of the extension event: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            } finally {
                context.setWikiReference(currentWiki);
            }
        }
    }
}
