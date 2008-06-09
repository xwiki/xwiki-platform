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
package com.xpn.xwiki.aspect.log;

import org.aspectj.lang.reflect.*;
import org.aspectj.lang.*;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * Log every entry and exit of methods. This is useful for debugging XWiki, especially in the
 * case when you cannot debug the application (for example when you cannot reproduce the problem
 * on your machine you can send an aspectified XWiki JAR and ask the user to run it and send you
 * the logs). As this logs everything you'll need to control what is logged by tuning the Log4J
 * configuration file to log only what you want.
 *
 * @version $Id$
 */
public aspect LogAspect
{
    private static final boolean ISACTIVE =
        Boolean.parseBoolean(System.getProperty("debug.active", "false"));
    private static final long TIMER_MIN = Long.parseLong(System.getProperty("debug.minTime", "100"));
    private static final boolean SHOW_DETAILS =
        Boolean.parseBoolean(System.getProperty("debug.showDetails", "true"));

    private int depth = 1;
    
    /**
     * All objects in the log package. We don't want to log these as they are
     * the object that perform the logging and thus at execution time we would
     * enter an infinite recursive loop.
     */
    pointcut logObjectCalls() :
        execution(public * com.xpn.xwiki.aspect.log..*(..));

    /**
     * All public static methods that have parameters.
     */
    pointcut publicStaticMethodsWithParameterCalls() :
        !execution(public static * com.xpn.xwiki..*())
        && execution(public static * com.xpn.xwiki..*(..));

    /**
     * All public methods that have parameters.
     */
    pointcut publicMethodsWithParameterCalls() :
        !execution(public * com.xpn.xwiki..*())
        && execution(public * com.xpn.xwiki..*(..));

    /**
     * All public methods that return values
     */
    pointcut publicMethodsWithReturnValueCalls() :
        !execution(public void com.xpn.xwiki..*(..))
        && execution(public * com.xpn.xwiki..*(..));

    /**
     * Log all entries and exits of static methods that have no return values.
     */
    Object around() :
        !logObjectCalls()
        && publicMethodsWithParameterCalls()
        && publicStaticMethodsWithParameterCalls()
        && !publicMethodsWithReturnValueCalls()
    {
        if (ISACTIVE) {
            // Get The logger to perform logging
            Logger logger = Logger.getLogger(thisJoinPoint.getSignature().getDeclaringType());

            if (logger.isDebugEnabled()) {

                // Execute the method
                long startTime = System.currentTimeMillis();
                depth++;
                final Object result = proceed();
                depth--;
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > TIMER_MIN) {
                    String prefix = '[' + (endTime - startTime) + ']'
                        + StringUtils.repeat(".", depth);
                    if (SHOW_DETAILS) {
                        logger.debug(prefix + getFullSignature(thisJoinPoint));
                    } else {
                        logger.debug(prefix + thisJoinPoint.getSignature().getName());
                    }
                }

                return result;
            }
        }
        return proceed();
    }

    /**
     * Log all entries and exits of non-static methods that have no return
     * values.
     */
    Object around() :
        !logObjectCalls()
        && publicMethodsWithParameterCalls()
        && !publicStaticMethodsWithParameterCalls()
        && !publicMethodsWithReturnValueCalls()
    {
        if (ISACTIVE) {
            // The class that uses the method that has been called
            final Class target = thisJoinPoint.getTarget().getClass();

            // Get The logger to perform logging
            Logger logger = Logger.getLogger(target);

            if (logger.isDebugEnabled()) {

                // Execute the method
                long startTime = System.currentTimeMillis();
                depth++;
                final Object result = proceed();
                depth--;
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > TIMER_MIN) {
                    String prefix = '[' + (endTime - startTime) + ']'
                        + StringUtils.repeat(".", depth);
                    if (SHOW_DETAILS) {
                        logger.debug(prefix + getFullSignature(thisJoinPoint));
                    } else {
                        logger.debug(prefix + thisJoinPoint.getSignature().getName());
                    }
                }

                return result;
            }
        }
        return proceed();
    }

    /**
     * Log all entries and exits of static methods that have return values.
     */
    Object around() :
        !logObjectCalls()
        && publicMethodsWithParameterCalls()
        && publicMethodsWithReturnValueCalls()
        && publicStaticMethodsWithParameterCalls()
    {
        if (ISACTIVE) {
            // Get The logger to perform logging
            Logger logger = Logger.getLogger(thisJoinPoint.getSignature().getDeclaringType());

            if (logger.isDebugEnabled()) {

                // Execute the method
                long startTime = System.currentTimeMillis();
                depth++;
                final Object result = proceed();
                depth--;
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > TIMER_MIN) {
                    String prefix = '[' + (endTime - startTime) + ']'
                        + StringUtils.repeat(".", depth);
                    if (SHOW_DETAILS) {

                        final StringBuffer returnValue =
                            new StringBuffer(thisJoinPoint.getSignature().getName());
                        returnValue.append(' ');
                        returnValue.append('=');
                        returnValue.append(' ');
                        returnValue.append('[');
                        returnValue.append(result);
                        returnValue.append(']');

                        logger.debug(prefix + getFullSignature(thisJoinPoint)
                            + returnValue.toString()); 
                    } else {
                        logger.debug(prefix + thisJoinPoint.getSignature().getName());
                    }
                }

                return result;
            }
        }
        return proceed();
    }

    /**
     * Log all entries and exits of non-static methods that have return values.
     */
    Object around() :
        !logObjectCalls()
        && publicMethodsWithParameterCalls()
        && publicMethodsWithReturnValueCalls()
        && !publicStaticMethodsWithParameterCalls()
    {
        if (ISACTIVE) {
            // The class that uses the method that has been called
            final Class target = thisJoinPoint.getTarget().getClass();

            // Get The logger to perform logging
            Logger logger = Logger.getLogger(target);

            if (logger.isDebugEnabled()) {

                // Execute the method
                long startTime = System.currentTimeMillis();
                depth++;
                final Object result = proceed();
                depth--;
                long endTime = System.currentTimeMillis();

                if (endTime - startTime > TIMER_MIN) {
                    String prefix = '[' + (endTime - startTime) + ']'
                        + StringUtils.repeat(".", depth);
                    if (SHOW_DETAILS) {

                        final StringBuffer returnValue =
                            new StringBuffer(thisJoinPoint.getSignature().getName());
                        returnValue.append(' ');
                        returnValue.append('=');
                        returnValue.append(' ');
                        returnValue.append('[');
                        returnValue.append(result);
                        returnValue.append(']');

                        logger.debug(prefix + getFullSignature(thisJoinPoint)
                            + returnValue.toString());
                    } else {
                        logger.debug(prefix + thisJoinPoint.getSignature().getName());
                    }
                }

                return result;
            }
        }
        return proceed();
    }

    /**
     * @return the full signature of a method
     */
    private final String getFullSignature(final JoinPoint jp)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(jp.getSignature().getName());
        buffer.append('(');
        final Object[] objs = jp.getArgs();
        if (objs.length > 0)
        {
            for (int i = 0; i < objs.length - 1; i++)
            {
                buffer.append('[');
                buffer.append(objs[i]);
                buffer.append(']');
                buffer.append(',');
                buffer.append(' ');
            }
            buffer.append('[');
            buffer.append(objs[objs.length - 1]);
            buffer.append(']');
        }
        buffer.append(')');
        return buffer.toString();
    }

}
