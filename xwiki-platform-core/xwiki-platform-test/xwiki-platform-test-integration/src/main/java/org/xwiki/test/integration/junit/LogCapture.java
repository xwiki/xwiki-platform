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
package org.xwiki.test.integration.junit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.io.output.TeeOutputStream;

/**
 * Captures stdout and stderr while still outputting content to them.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class LogCapture
{
    private PrintStream savedOut;

    private PrintStream savedErr;

    private ByteArrayOutputStream collectingContentStream;

    /**
     * Start capturing stdout/stderr.
     */
    public void startCapture()
    {
        this.savedOut = System.out;
        this.savedErr = System.err;
        this.collectingContentStream = new ByteArrayOutputStream();

        // Capture stdout but continue sending data to it at the same time
        System.setOut(new PrintStream(new TeeOutputStream(this.collectingContentStream, this.savedOut)));

        // Capture stderr but continue sending data to it at the same time
        System.setErr(new PrintStream(new TeeOutputStream(this.collectingContentStream, this.savedErr)));
    }

    /**
     * Stop capturing stdout/stderr.
     *
     * @return the captured output
     */
    public String stopCapture()
    {
        // Put back stdout
        System.setOut(this.savedOut);

        // Put back stderr
        System.setOut(this.savedErr);

        String outputContent = this.collectingContentStream.toString();
        return outputContent;
    }
}
