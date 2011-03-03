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
package org.xwiki.velocity.internal;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Default implementation of the Velocity service which initializes the Velocity system using configuration values
 * defined in the component's configuration. Note that the {@link #initialize} method has to be executed before any
 * other method can be called.
 * 
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultVelocityEngine extends AbstractLogEnabled implements VelocityEngine, LogChute
{
    /**
     * Used to set it as a Velocity Application Attribute so that Velocity extensions done by XWiki can use it to
     * lookup other components.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Velocity configuration to get the list of configured Velocity properties.
     */
    @Requirement
    private VelocityConfiguration velocityConfiguration;

    /**
     * The Velocity engine we're wrapping.
     */
    private org.apache.velocity.app.VelocityEngine engine;

    /**
     * See the comment in {@link #init(org.apache.velocity.runtime.RuntimeServices)}.
     */
    private RuntimeServices rsvc;

    /** Counter for the number of active rendering processes using each namespace. */
    private final Map<String, Integer> namespaceUsageCount = new HashMap<String, Integer>();

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#initialize(Properties)
     */
    public void initialize(Properties overridingProperties) throws XWikiVelocityException
    {
        org.apache.velocity.app.VelocityEngine velocityEngine = new org.apache.velocity.app.VelocityEngine();

        // Add the Component Manager to allow Velocity extensions to lookup components.
        velocityEngine.setApplicationAttribute(ComponentManager.class.getName(), this.componentManager);

        initializeProperties(velocityEngine, this.velocityConfiguration.getProperties(), overridingProperties);

        try {
            velocityEngine.init();
        } catch (Exception e) {
            throw new XWikiVelocityException("Cannot start the Velocity engine", e);
        }

        this.engine = velocityEngine;
    }

    /**
     * @param velocityEngine the Velocity engine against which to initialize Velocity properties
     * @param configurationProperties the Velocity properties coming from XWiki's configuration
     * @param overridingProperties the Velocity properties that override the properties coming from XWiki's
     *        configuration
     */
    private void initializeProperties(org.apache.velocity.app.VelocityEngine velocityEngine,
        Properties configurationProperties, Properties overridingProperties)
    {
        // Avoid "unable to find resource 'VM_global_library.vm' in any resource loader." if no
        // Velocimacro library is defined. This value is overriden below.
        velocityEngine.setProperty("velocimacro.library", "");

        velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, this);

        // Configure Velocity by passing the properties defined in this component's configuration
        if (configurationProperties != null) {
            for (Enumeration< ? > e = configurationProperties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                // Only set a property if it's not overridden by one of the passed properties
                if (!overridingProperties.containsKey(key)) {
                    String value = configurationProperties.getProperty(key);
                    velocityEngine.setProperty(key, value);
                    getLogger().debug("Setting property [" + key + "] = [" + value + "]");
                }
            }
        }

        // Override the component's static properties with the ones passed in parameter
        if (overridingProperties != null) {
            for (Enumeration< ? > e = overridingProperties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                String value = overridingProperties.getProperty(key);
                velocityEngine.setProperty(key, value);
                getLogger().debug("Overriding property [" + key + "] = [" + value + "]");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#evaluate(Context, java.io.Writer, String, String)
     */
    public boolean evaluate(Context context, Writer out, String templateName, String source)
        throws XWikiVelocityException
    {
        return evaluate(context, out, templateName, new StringReader(source));
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#evaluate(Context, java.io.Writer, String, String)
     * @see #init(RuntimeServices)
     */
    public boolean evaluate(Context context, Writer out, String templateName, Reader source)
        throws XWikiVelocityException
    {
        // Ensure that initialization has been called
        if (this.engine == null) {
            throw new XWikiVelocityException("This Velocity Engine has not yet been initialized. "
                + " You must call its initialize() method before you can use it.");
        }

        // We override the default implementation here. See #init(RuntimeServices)
        // for explanations.
        try {
            SimpleNode nodeTree = null;

            // The trick is done here: We use the signature that allows
            // passing a boolean and we pass false, thus preventing Velocity
            // from cleaning the context of its velocimacros even though the
            // config property velocimacro.permissions.allow.inline.local.scope
            // is set to true.
            nodeTree = this.rsvc.parse(source, templateName, false);

            if (nodeTree != null) {
                InternalContextAdapterImpl ica = new InternalContextAdapterImpl(context);
                ica.pushCurrentTemplateName(templateName);
                try {
                    nodeTree.init(ica, this.rsvc);
                    nodeTree.render(ica, out);
                } finally {
                    ica.popCurrentTemplateName();
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            throw new XWikiVelocityException("Failed to evaluate content with id [" + templateName + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#clearMacroNamespace(String)
     * @since 2.4M2
     */
    public void clearMacroNamespace(String templateName)
    {
        this.rsvc.dumpVMNamespace(templateName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#startedUsingMacroNamespace(String)
     * @since 2.4RC1
     */
    public void startedUsingMacroNamespace(String namespace)
    {
        synchronized (this.namespaceUsageCount) {
            Integer count = this.namespaceUsageCount.get(namespace);
            if (count == null) {
                count = Integer.valueOf(0);
            }
            count = count + 1;
            this.namespaceUsageCount.put(namespace, count);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityEngine#stoppedUsingMacroNamespace(String)
     * @since 2.4RC1
     */
    public void stoppedUsingMacroNamespace(String namespace)
    {
        synchronized (this.namespaceUsageCount) {
            Integer count = this.namespaceUsageCount.get(namespace);
            if (count == null) {
                // This shouldn't happen
                this.log(LogChute.WARN_ID, "Wrong usage count for namespace [" + namespace + "]");
                return;
            }
            count = count - 1;
            if (count <= 0) {
                this.namespaceUsageCount.remove(namespace);
                this.clearMacroNamespace(namespace);
            } else {
                this.namespaceUsageCount.put(namespace, count);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LogChute#init(org.apache.velocity.runtime.RuntimeServices)
     */
    public void init(RuntimeServices runtimeServices)
    {
        // We save the RuntimeServices instance in order to be able to override the
        // VelocityEngine.evaluate() method. We need to do this so that it's possible
        // to make macros included with #includeMacros() work even though we're using
        // the Velocity setting:
        // velocimacro.permissions.allow.inline.local.scope = true
        // TODO: Fix this when by rewriting the XWiki.include() implementation so that
        // included Velocity templates are added to the current document before
        // evaluation instead of doing 2 separate executions.
        this.rsvc = runtimeServices;
    }

    /**
     * {@inheritDoc}
     * 
     * @see LogChute#log(int, String)
     */
    public void log(int level, String message)
    {
        switch (level) {
            case LogChute.WARN_ID:
                getLogger().warn(message);
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                getLogger().debug(message);
                break;
            case LogChute.DEBUG_ID:
                getLogger().debug(message);
                break;
            case LogChute.ERROR_ID:
                getLogger().error(message);
                break;
            default:
                getLogger().debug(message);
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LogChute#log(int, String, Throwable)
     */
    public void log(int level, String message, Throwable throwable)
    {
        switch (level) {
            case LogChute.WARN_ID:
                getLogger().warn(message, throwable);
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                getLogger().debug(message, throwable);
                break;
            case LogChute.DEBUG_ID:
                getLogger().debug(message, throwable);
                break;
            case LogChute.ERROR_ID:
                getLogger().error(message, throwable);
                break;
            default:
                getLogger().debug(message, throwable);
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LogChute#isLevelEnabled(int)
     */
    public boolean isLevelEnabled(int level)
    {
        boolean isEnabled;

        switch (level) {
            case LogChute.WARN_ID:
                isEnabled = getLogger().isWarnEnabled();
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                isEnabled = getLogger().isDebugEnabled();
                break;
            case LogChute.DEBUG_ID:
                isEnabled = getLogger().isDebugEnabled();
                break;
            case LogChute.ERROR_ID:
                isEnabled = getLogger().isErrorEnabled();
                break;
            default:
                isEnabled = getLogger().isDebugEnabled();
                break;
        }

        return isEnabled;
    }
}
