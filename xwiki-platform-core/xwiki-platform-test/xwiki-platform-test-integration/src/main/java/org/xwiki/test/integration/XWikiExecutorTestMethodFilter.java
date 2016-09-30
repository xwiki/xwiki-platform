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
package org.xwiki.test.integration;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * Filter to be applied on {@link XWikiExecutorSuite}'s executed {@link org.junit.runner.Runner}s (i.e. test
 * classes).
 * <p>
 * Each child runner (i.e. test method) of a test class is compared with a {@link #pattern} to see if it
 * {@link #shouldRun(Description)} during this test execution.
 * <p>
 * The "pattern" System Property is a regex that gets applied to values such as:
 * <ul>
 * <li>pa.ck.a.ge.Class</li>
 * <li>pa.ck.a.ge.Class#method</li>
 * </ul>
 * <p>
 * Examples of valid patterns:
 * <dl>
 * <dt>-Dpattern="Test1"
 * <dd>run all tests from the matching test class(es) (that contain(s) "Test1" in its name)
 * <dt>-Dpattern="Test1|Test2"
 * <dd>run all tests from 2 test classes
 * <dt>-Dpattern="Test1#method1"
 * <dd>run just matching method(s) from matching test class(es)
 * <dt>-Dpattern="Test1#method1|Test2#method2"
 * <dd>run just matching methods from matching test classes (more than just 1 class)
 * <dt>-Dpattern="Test1#method1|Test2"
 * <dd>mix it; run just matching methods from the first matching class(es) and ALL tests from the second matching
 * class(es)
 * </dl>
 * 
 * @version $Id$
 * @since 6.2
 */
public class XWikiExecutorTestMethodFilter extends Filter
{
    protected String pattern;

    /**
     * Constructor.
     * 
     * @param pattern the pattern to use when filtering a runner's children (i.e. test methods).
     */
    public XWikiExecutorTestMethodFilter(String pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @return the pattern to use when filtering a {@link org.junit.runner.Runner}'s children (i.e. test methods).
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @param pattern the new pattern to use.
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public boolean shouldRun(Description description)
    {
        String testMethodName = String.format("%s#%s", description.getClassName(), description.getMethodName());
        boolean result = testMethodName.matches(this.pattern);

        return result;
    }

    @Override
    public String describe()
    {
        return "Run only the tests specified with the -Dpattern parameter.";
    }
}
