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
package org.xwiki.localization.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.localization.Bundle;

/**
 * Bundle corresponding to the pulled filesystem resource bundles. To pull a resource, call
 * <code>$l10n.use("resource", "ModuleResources")</code>
 * 
 * @version $Id$
 */
@Component("resource")
public class PulledFilesBundle extends AbstractFilesystemBundle implements Bundle, Initializable
{
    /** The key used for placing the list of pulled file bundles in the current execution context. */
    public static final String PULLED_CONTEXT_KEY = PulledFilesBundle.class.getName() + "_bundles";

    /** Provides access to the request context. */
    @Requirement
    protected Execution execution;

    @Override
    public void initialize() throws InitializationException
    {
        // Set the Bundle priority
        setPriority(200);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Bundle#getTranslation(String, String)
     */
    @SuppressWarnings("unchecked")
    public String getTranslation(String key, String language)
    {
        String translation = key;
        // The list of pulled resources is taken from the execution context.
        List<String> fileNames = (List<String>) this.execution.getContext().getProperty(PULLED_CONTEXT_KEY);
        if (fileNames != null) {
            Properties props;
            synchronized (fileNames) {
                for (String fileName : fileNames) {
                    try {
                        props = getFileBundle(fileName, language);
                        if (props.containsKey(key)) {
                            translation = props.getProperty(key);
                            // The first translation found is returned.
                            break;
                        }
                    } catch (Exception e) {
                        getLogger().warn("Cannot load resource bundle: [{0}]", fileName);
                    }
                }
            }
        }
        return translation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Bundle#use(String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void use(String bundleLocation)
    {
        // In theory, the execution context should not be shared by more than one thread. But just in case...
        synchronized (this.execution.getContext()) {
            List<String> fileNames = (List<String>) this.execution.getContext().getProperty(PULLED_CONTEXT_KEY);
            if (fileNames == null) {
                fileNames = new ArrayList<String>();
                this.execution.getContext().setProperty(PULLED_CONTEXT_KEY, fileNames);
            }
            synchronized (fileNames) {
                fileNames.add(bundleLocation);
            }
        }
    }
}
