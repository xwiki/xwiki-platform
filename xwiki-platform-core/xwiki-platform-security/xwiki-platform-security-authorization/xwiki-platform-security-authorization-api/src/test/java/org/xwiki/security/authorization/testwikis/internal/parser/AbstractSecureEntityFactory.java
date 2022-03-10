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

package org.xwiki.security.authorization.testwikis.internal.parser;

import org.xwiki.security.authorization.testwikis.TestEntity;

/**
 * Base factory for the creation of test entities supporting access rules based on an XML definition.
 *
 * @param <T> Specialized type of the entity created by this factory
 *
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractSecureEntityFactory<T extends TestEntity> extends AbstractEntityFactory<T>
{
    /**
     * Create this entity factory. Since most subclasses constructor override the default parented constructor, this
     * one is used by default.
     */
    AbstractSecureEntityFactory() {
    }

    /**
     * Create this entity factory. Since most subclasses constructor override this constructor, this one is never use.
     * @param parent Parent entity to be used for all entities created by this factory.
     */
    AbstractSecureEntityFactory(TestEntity parent) {
        setParent(parent);
    }

    @Override
    protected void registerFactories(ElementParser parser, T entity)
    {
        super.registerFactories(parser, entity);
        parser.register(new TestAccessRuleFactory(entity));
    }
}
