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
package org.xwiki.security.authorization;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test Set interface of RightSet using guava test library.
 *
 * @version $Id$
 * @since 4.0M2
 */
@RunWith(AllTests.class)
public class RightSetTest
{
    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(SetTestSuiteBuilder.using(new TestSetGenerator<Right>() {
                @Override
                public SampleElements<Right> samples()
                {
                    return new SampleElements<Right>(
                        Right.VIEW,
                        Right.EDIT,
                        Right.DELETE,
                        Right.COMMENT,
                        Right.ADMIN
                    );
                }

                @Override
                public Set<Right> create(Object... elements)
                {
                    Set<Right> set = new RightSet();
                    for (Object e : elements) {
                        set.add((Right) e);
                    }
                    return set;
                }

                @Override
                public Right[] createArray(int length)
                {
                    return new Right[length];
                }

                @Override
                public Iterable<Right> order(List<Right> insertionOrder)
                {
                    return new TreeSet<Right>(insertionOrder);
                }
            })
            .named("RightSet")
            .withFeatures(SetFeature.GENERAL_PURPOSE,
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_QUERIES,
                CollectionFeature.KNOWN_ORDER)
            .suppressing(com.google.common.collect.testing.testers.SetHashCodeTester.getHashCodeMethods())
            .createTestSuite());
        return suite;
    }
}
