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

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Base class to initialize xclass for various implementations of UI extensions.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractAsyncClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Indicate if the asynchronous execution is allowed for this extension.
     */
    public static final String XPROPERTY_ASYNC_ENABLED = "async_enabled";

    /**
     * Indicate if caching is allowed for this UI extension.
     */
    public static final String XPROPERTY_ASYNC_CACHED = "async_cached";

    /**
     * Indicate the list of context elements required by the UI extension execution.
     */
    public static final String XPROPERTY_ASYNC_CONTEXT = "async_context";

    @Inject
    private ContextStoreManager contextStore;

    @Inject
    private LocalizationManager localization;

    @Inject
    private Logger logger;

    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     */
    public AbstractAsyncClassDocumentInitializer(EntityReference reference)
    {
        super(reference);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addBooleanField(XPROPERTY_ASYNC_ENABLED, "Asynchronous rendering", null, Boolean.FALSE);
        xclass.addBooleanField(XPROPERTY_ASYNC_CACHED, "Cached", null, Boolean.FALSE);

        // TODO: move all that in a custom displayer to be less static
        Collection<String> contextEntries;
        try {
            contextEntries = this.contextStore.getSupportedEntries();
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get supported context entries", e);

            contextEntries = Collections.emptyList();
        }
        StringBuilder entriesString = new StringBuilder();
        for (String entry : contextEntries) {
            if (entriesString.length() > 0) {
                entriesString.append('|');
            }
            entriesString.append(entry);

            String translation = this.localization.getTranslationPlain("rendering.async.context.entry." + entry,
                this.localization.getDefaultLocale());
            if (translation != null) {
                entriesString.append('=');
                entriesString.append(translation);
            }
        }

        StaticListClass asyncClass = xclass.addStaticListField(XPROPERTY_ASYNC_CONTEXT);
        asyncClass.setPrettyName("Context elements");
        asyncClass.setSize(5);
        asyncClass.setMultiSelect(true);
        asyncClass.setValues(entriesString.toString());
        asyncClass.setSeparator(", ");
    }
}
