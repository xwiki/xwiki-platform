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
package org.xwiki.store.hibernate.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Make sure the methods added to the Hibernate and Jakarta Persistence types don't prevent mocking them (which is
 * what most of the tests of the code using Hibernate do).
 *
 * @version $Id$
 */
class PatchedTypesMockTest
{
    @ParameterizedTest
    @ValueSource(classes = {Session.class, SessionImplementor.class, SessionFactory.class, QueryProducer.class,
        Query.class, SelectionQuery.class, NativeQuery.class, org.hibernate.Query.class, SQLQuery.class,
        Transaction.class, HibernateCriteriaBuilder.class, EntityManager.class, TypedQuery.class,
        CriteriaQuery.class, Root.class})
    void mockImplementsAllMethods(Class<?> type) throws IllegalAccessException
    {
        Object mock = mock(type);

        List<String> notImplemented = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                try {
                    method.invoke(mock, defaultValues(method.getParameterTypes()));
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof AbstractMethodError) {
                        notImplemented.add(method.toString());
                    }
                }
            }
        }

        assertEquals(List.of(), notImplemented);
    }

    private static Object[] defaultValues(Class<?>[] types)
    {
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; ++i) {
            values[i] = types[i].isPrimitive() ? Array.get(Array.newInstance(types[i], 1), 0) : null;
        }

        return values;
    }
}
