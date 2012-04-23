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
package org.xwiki.groovy.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.groovy.GroovyCompilationCustomizer;

/**
 * Provides a sandbox environment for running Groovy scripts in a safe way.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Named("secure")
@Singleton
public class SecureGroovyCompilationCustomizer implements GroovyCompilationCustomizer
{
    /**
     * Used to check for Programming Rights; if the document has Programming Rights then don't perform any check.
     */
    @Inject
    private DocumentAccessBridge dab;

    /**
     * Prevents executing {@code System.exit}.
     */
    private class SystemExitChecker implements SecureASTCustomizer.ExpressionChecker
    {
        @Override
        public boolean isAuthorized(Expression expression)
        {
            boolean result = true;

            if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;
                if (methodCallExpression.getObjectExpression() instanceof ClassExpression) {
                    ClassExpression classExpression = (ClassExpression) methodCallExpression.getObjectExpression();
                    if (classExpression.getType().getName().equals(System.class.getName())
                        && methodCallExpression.getMethodAsString().equals("exit"))
                    {
                        result = false;
                    }
                }
            }

            return result;
        }
    }

    @Override
    public CompilationCustomizer createCustomizer()
    {
        CompilationCustomizer customizer = null;
        if (!this.dab.hasProgrammingRights()) {
            SecureASTCustomizer secureCustomizer = new SecureASTCustomizer();
            secureCustomizer.addExpressionCheckers(new SystemExitChecker());
            customizer = secureCustomizer;
        }
        return customizer;
    }
}
