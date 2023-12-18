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
package org.xwiki.evaluation.internal;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.evaluation.ObjectEvaluator;
import org.xwiki.evaluation.ObjectEvaluatorException;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Evaluator that proxies the actual evaluation to the right ObjectEvaluator implementation, based on the XClass of
 * the object.
 *
 * @version $Id$
 * @since 14.10.21
 * @since 15.5.5
 * @since 15.10.2
 */
@Component
@Singleton
public class DefaultObjectEvaluator implements ObjectEvaluator
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public Map<String, String> evaluate(BaseObject object) throws ObjectEvaluatorException
    {
        if (object == null) {
            return Collections.emptyMap();
        }

        String xClassName = this.entityReferenceSerializer.serialize(object.getXClassReference());
        ComponentManager componentManager = this.contextComponentManagerProvider.get();
        if (!componentManager.hasComponent(ObjectEvaluator.class, xClassName)) {
            throw new ObjectEvaluatorException(String.format("Could not find an instance of 'ObjectEvaluator' for "
                + "XObject of class '%s'.", xClassName));
        }

        try {
            ObjectEvaluator objectEvaluator = componentManager.getInstance(ObjectEvaluator.class, xClassName);
            return objectEvaluator.evaluate(object);
        } catch (ComponentLookupException e) {
            throw new ObjectEvaluatorException(String.format("Could not instantiate 'ObjectEvaluator' for XObject of "
                + "class '%s'.", xClassName), e);
        }
    }
}
