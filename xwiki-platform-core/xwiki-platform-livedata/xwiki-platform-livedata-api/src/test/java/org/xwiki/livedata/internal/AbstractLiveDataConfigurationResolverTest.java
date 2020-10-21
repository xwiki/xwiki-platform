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
package org.xwiki.livedata.internal;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.xwiki.livedata.LiveDataConfigurationResolver;

/**
 * Base class for {@link LiveDataConfigurationResolver} tests.
 * 
 * @version $Id$
 * @since 12.9
 */
class AbstractLiveDataConfigurationResolverTest
{
    protected static Stream<String[]> getTestData(File file) throws Exception
    {
        List<String[]> testData = new ArrayList<>();
        ListIterator<String> linesIterator = IOUtils.readLines(new FileReader(file)).listIterator();
        while (linesIterator.hasNext()) {
            String message = readTestMessage(linesIterator);
            String input = readTestInput(linesIterator);
            String output = readTestOutput(linesIterator);
            testData.add(new String[] {message, input, output});
        }
        return testData.stream();
    }

    private static String readTestMessage(ListIterator<String> linesIterator)
    {
        StringBuilder message = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (line.startsWith("##")) {
                message.append(line.substring(2).trim());
            } else {
                linesIterator.previous();
                break;
            }
        }
        return message.toString();
    }

    private static String readTestInput(ListIterator<String> linesIterator)
    {
        StringBuilder input = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (!line.equals("---")) {
                input.append(line.trim());
            } else {
                break;
            }
        }
        return input.toString();
    }

    private static String readTestOutput(ListIterator<String> linesIterator)
    {
        StringBuilder output = new StringBuilder();
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            if (!line.startsWith("##")) {
                output.append(line.trim());
            } else {
                linesIterator.previous();
                break;
            }
        }
        return output.toString();
    }
}
