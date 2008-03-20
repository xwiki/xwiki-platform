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
 *
 */
package org.xwiki.velocity;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;

/**
 * Default implementation for {@link VelocityFactory}. 
 * @see VelocityFactory
 */
public class DefaultVelocityFactory extends AbstractLogEnabled implements VelocityFactory, Composable
{
	private ComponentManager componentManager;
	
	private Map<String, VelocityManager> velocityManagers = new HashMap<String, VelocityManager>();

	/**
	 * {@inheritDoc}
	 * @see Composable#compose(ComponentManager)
	 */
	public void compose(ComponentManager componentManager)
	{
		this.componentManager = componentManager;
	}

	/**
	 * {@inheritDoc}
	 * @see VelocityFactory#hasVelocityManager(String)
	 */
	public boolean hasVelocityManager(String key)
	{
		return this.velocityManagers.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 * @see VelocityFactory#getVelocityManager(String)
	 */
	public VelocityManager getVelocityManager(String key)
	{
		return this.velocityManagers.get(key);
	}
	
	/**
	 * {@inheritDoc}
	 * @see VelocityFactory#createVelocityManager(String, Properties)
	 */
	public VelocityManager createVelocityManager(String key, Properties properties) throws XWikiVelocityException
	{
		VelocityManager manager;
		if (this.velocityManagers.containsKey(key)) {
			manager = this.velocityManagers.get(key);
		} else {
			try {
				manager = (VelocityManager) this.componentManager.lookup(VelocityManager.ROLE);
			} catch (ComponentLookupException e) {
				throw new XWikiVelocityException("Failed to create Velocity Manager", e);
			}
			manager.initialize(properties);
			this.velocityManagers.put(key, manager);
		}
		return manager;
	}
}
