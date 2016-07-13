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
package org.xwiki.test.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.xwiki.test.integration.XWikiExecutorSuite;

/**
 * Extends {@link XWikiExecutorSuite} and initialize the {@link PersistentTestContext}.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class PageObjectSuite extends XWikiExecutorSuite
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface PostStart
    {
    }
    
    private PersistentTestContext context;

    public PageObjectSuite(Class< ? > suiteClass, RunnerBuilder builder) throws InitializationError
    {
        super(suiteClass, builder);
    }

    @Override
    protected void beforeTests()
    {
        // Construct the executors and start the XWiki instances.
        super.beforeTests();

        try {
            initializePersistentTestContext();

            // Callback to setup executors in the suite class after containers have been started
            for (Method method : getTestClass().getJavaClass().getMethods()) {
                PostStart postStartAnnotation = method.getAnnotation(PostStart.class);
                if (postStartAnnotation != null) {
                    // Call it!
                    Object instance = getTestClass().getJavaClass().newInstance();
                    method.invoke(instance, this.context);
                }
            }
        } catch (Exception e) {
            // Make sure to stop the executors if anything goes wrong
            afterTests();

            throw new RuntimeException("Failed to initialize PO suite", e);
        }
    }

    private void initializePersistentTestContext()
    {
        try {
            this.context = new PersistentTestContext(getExecutors());
            AbstractTest.initializeSystem(this.context.getUnstoppable());

            // Cache the initial CSRF token since that token needs to be passed to all forms (this is done automatically
            // in TestUtils), including the login form. Whenever a new user logs in we need to recache.
            // Note that this requires a running XWiki instance.
            AbstractTest.context.getUtil().recacheSecretToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PersistentTestContext", e);
        }
    }

    @Override
    protected void afterTests()
    {
        if (context != null) {
            try {
                context.shutdown();
            } catch (Exception e) {
                throw new RuntimeException("Failed to shutdown PersistentTestContext", e);
            }
        }

        // Note: Don't call super.afterTests() since XWiki will be stopped twice otherwise!
    }
}
