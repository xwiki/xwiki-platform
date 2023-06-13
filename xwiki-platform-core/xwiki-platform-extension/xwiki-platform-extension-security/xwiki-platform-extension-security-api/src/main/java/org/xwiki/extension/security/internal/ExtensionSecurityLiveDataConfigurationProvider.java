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
package org.xwiki.extension.security.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;

import static org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;

/**
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityLiveDataSource.ID)
public class ExtensionSecurityLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    /**
     * The name LD field.
     */
    public static final String NAME = "name";

    /**
     * Extension ID LD field.
     */
    public static final String EXTENSION_ID = "extensionId";

    /**
     * Max CVSS LD field.
     */
    public static final String MAX_CVSS = "maxCVSS";

    /**
     * The fix version field.
     */
    public static final String FIX_VERSION = "fixVersion";

    /**
     * The advice field.
     */
    public static final String ADVICE = "advice";

    /**
     * Extension ID LD field.
     */
    public static final String CVE_ID = "cveID";

    private static final String STRING_TYPE = "String";

    private static final String HTML_DISPLAYER_ID = "html";

    @Override
    public LiveDataConfiguration get()
    {
        LiveDataConfiguration input = new LiveDataConfiguration();
        LiveDataMeta meta = new LiveDataMeta();
        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty(ExtensionSecurityLiveDataSource.ID);
        input.setMeta(meta);
        meta.setEntryDescriptor(entryDescriptor);
        meta.setPropertyDescriptors(List.of(
            initExtensionNameDescriptor(),
            initExtensionIdDescriptor(),
            initMaxCVSSDescriptor(),
            initCVEIDDescriptor(),
            initFixVersionDescriptor(),
            initAdviceDescriptor()
        ));
        return input;
    }

    private LiveDataPropertyDescriptor initExtensionNameDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("Name");
        descriptor.setId(NAME);
        descriptor.setType(STRING_TYPE);
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);
        descriptor.setDisplayer(new DisplayerDescriptor(HTML_DISPLAYER_ID));
        return descriptor;
    }

    private static LiveDataPropertyDescriptor initExtensionIdDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("Extension Id");
        descriptor.setId(EXTENSION_ID);
        descriptor.setType(STRING_TYPE);
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);
        return descriptor;
    }

    private LiveDataPropertyDescriptor initMaxCVSSDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("Max CVSS");
        descriptor.setId(MAX_CVSS);
        descriptor.setType("Double");
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);
        return descriptor;
    }

    private static LiveDataPropertyDescriptor initCVEIDDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("CVE ID");
        descriptor.setId(CVE_ID);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new DisplayerDescriptor(HTML_DISPLAYER_ID));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(false);
        descriptor.setFilterable(true);
        return descriptor;
    }

    private LiveDataPropertyDescriptor initFixVersionDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("Fix Version");
        descriptor.setId(FIX_VERSION);
        descriptor.setType(STRING_TYPE);
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(false);
        descriptor.setFilterable(true);
        return descriptor;
    }

    private LiveDataPropertyDescriptor initAdviceDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        // TODO: translate?
        descriptor.setName("Advice");
        descriptor.setId(ADVICE);
        descriptor.setType(STRING_TYPE);
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(false);
        descriptor.setFilterable(false);
        return descriptor;
    }
}
