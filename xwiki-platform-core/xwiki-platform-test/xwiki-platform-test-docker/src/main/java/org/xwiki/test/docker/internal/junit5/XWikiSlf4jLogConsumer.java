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
package org.xwiki.test.docker.internal.junit5;

import org.slf4j.Logger;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Custom extension of {@link Slf4jLogConsumer} to outut warnings and errors when container start and when verbose
 * is off.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class XWikiSlf4jLogConsumer extends Slf4jLogConsumer
{
    private boolean isVerbose;

    /**
     * @param logger the SLF4J logger to proxy to
     * @param isVerbose if true then log everything, otherwise only log warnings and errors
     */
    public XWikiSlf4jLogConsumer(Logger logger, boolean isVerbose)
    {
        super(logger);
        this.isVerbose = isVerbose;
    }

    @Override
    public void accept(OutputFrame outputFrame)
    {
        String utf8String = outputFrame.getUtf8String();
        utf8String = utf8String.replaceAll("((\\r?\\n)|(\\r))$", "");

        if (this.isVerbose || utf8String.contains("WARN") || utf8String.contains("ERROR")) {
            super.accept(outputFrame);
        }
    }
}
