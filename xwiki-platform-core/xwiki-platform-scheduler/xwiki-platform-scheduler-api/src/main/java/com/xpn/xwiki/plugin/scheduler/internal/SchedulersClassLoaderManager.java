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
package com.xpn.xwiki.plugin.scheduler.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.NamespaceURLClassLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;

/**
 * Component dedicated to handle operations related to loading classes for Scheduler.
 *
 * @version $Id$
 * @since 17.10.1
 * @since 18.0.0RC1
 */
@Component(roles = SchedulersClassLoaderManager.class)
@Singleton
public class SchedulersClassLoaderManager
{
    private SchedulerPlugin schedulerPlugin;

    private final Map<String, Set<BaseObjectReference>> schedulersMapPerNamespace = new HashMap<>();

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;
    
    @Inject
    private ClassLoaderManager classLoaderManager;

    /**
     * Define the instance of the scheduler plugin to use.
     * @param schedulerPlugin the scheduler plugin instance this instance should use.
     */
    public void setSchedulerPlugin(SchedulerPlugin schedulerPlugin)
    {
        this.schedulerPlugin = schedulerPlugin;
    }

    private void registerScheduler(String namespace, BaseObjectReference objectReference)
    {
        if (!this.schedulersMapPerNamespace.containsKey(namespace)) {
            this.schedulersMapPerNamespace.put(namespace, new HashSet<>());
        }
        this.schedulersMapPerNamespace.get(namespace).add(objectReference);
    }

    /**
     * Remove scheduler information related to given object reference.
     * @param objectReference the reference of a scheduler object.
     */
    public void removeScheduler(BaseObjectReference objectReference)
    {
        for (Set<BaseObjectReference> objectReferenceSet : this.schedulersMapPerNamespace.values()) {
            objectReferenceSet.remove(objectReference);
        }
    }

    /**
     * Remove all schedulers information associated to a namespace.
     * @param namespace the namespace for which to remove information.
     */
    public void removeSchedulers(String namespace)
    {
        this.schedulersMapPerNamespace.remove(namespace);
    }

    /**
     * Perform operations when a classloader of a specific namespace is reset.
     * @param namespace the namespace for which an event has been triggered.
     */
    public void onClassLoaderReset(String namespace)
    {
        schedulersMapPerNamespace
            .getOrDefault(namespace, Set.of())
            .parallelStream()
            .forEach(this::reloadScheduler);
    }

    private void reloadScheduler(BaseObjectReference objectReference)
    {
        XWikiContext context = contextProvider.get();
        EntityReference documentReference = objectReference.extractReference(EntityType.DOCUMENT);
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);
            BaseObject jobObject = document.getXObject(SchedulerJobClassDocumentInitializer.XWIKI_JOB_CLASSREFERENCE);
            this.schedulerPlugin.unscheduleJob(jobObject, context);
            this.schedulerPlugin.scheduleJob(jobObject, context);
        } catch (XWikiException e) {
            this.logger.error("Error while trying to reload scheduler for object [{}]: ", objectReference, e);
        }
    }

    /**
     * Load a class for a scheduler and register it at the same time.
     * @param className the name of the class to load.
     * @param baseObjectReference the reference of the object of the scheduler.
     * @return the instance of the given class name.
     * @throws ClassNotFoundException if the class cannot be found.
     */
    public Class<?> loadClassAndRegister(String className, BaseObjectReference baseObjectReference)
        throws ClassNotFoundException
    {
        String namespace = null;

        // Reload the root classloader if needed: it's important if it's been dropped.
        NamespaceURLClassLoader classLoader = this.classLoaderManager.getURLClassLoader(null, true);
        Class<?> result = Class.forName(className, true, classLoader);

        // find the actual namespace of the classloader from where the class has been found.
        if (result.getClassLoader() instanceof NamespaceURLClassLoader namespaceURLClassLoader) {
            namespace = namespaceURLClassLoader.getNamespace();
        }

        this.registerScheduler(namespace, baseObjectReference);
        return result;
    }
}
