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
package org.xwiki.test.docker.junit5.libreoffice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to resolve the right version of LibreOffice.
 * 
 * @version $Id$
 * @since 18.5.0RC1
 * @since 18.4.1
 * @since 17.10.10
 */
public final class LibreOfficeResolver
{
    /**
     * The string to use to request the latest stable version.
     */
    public static final String LATEST = "latest";

    /**
     * The string to use to request the latest LTS version (latest version of the previous branch).
     */
    public static final String LTS = "lts";

    private static final String LIBREOFFICE_STABLE_URL = "https://download.documentfoundation.org/libreoffice/stable/";

    private static final Pattern LIBREOFFICE_VERSION_PATTERN = Pattern.compile("href=\"((\\d+)\\.(\\d+)\\.(\\d+))/\"");

    private LibreOfficeResolver()
    {
        // Utility class
    }

    private static String getDownloadPageBody() throws IOException, InterruptedException
    {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LIBREOFFICE_STABLE_URL)).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * @param tag the tag used to decide the version to pick. If the tag contains "latest", then the latest version be
     *            picked, "latest-lts" will pick the latest version of the previous branch.
     * @return the version of LibreOffice to use for the given tag.
     * @throws IOException if an error occurs while fetching the LibreOffice versions.
     * @throws InterruptedException if the thread is interrupted while fetching the LibreOffice versions.
     */
    public static String resolve(String tag) throws IOException, InterruptedException
    {
        String normalizedTag = tag;

        if (normalizedTag == null) {
            // Get the LTS by default
            normalizedTag = LTS;
        } else if (!LATEST.equals(tag) && !LTS.equals(tag)) {
            return tag;
        }

        String body = getDownloadPageBody();

        List<Version> versions = extractLibreOfficeVersions(body);
        if (versions.isEmpty()) {
            throw new IllegalStateException(
                String.format("Failed to find LibreOffice versions from [%s]", LIBREOFFICE_STABLE_URL));
        }

        List<Branch> branches =
            versions.stream().map(version -> new Branch(version.major(), version.minor())).distinct().sorted().toList();

        // Return the latest version is requested, or if no previous branch could be found
        if (LATEST.equals(normalizedTag) || branches.size() < 2) {
            return getLatestVersionOfBranch(versions, branches.get(branches.size() - 1)).toString();
        }

        return getLatestVersionOfBranch(versions, branches.get(branches.size() - 2)).toString();
    }

    private static Version getLatestVersionOfBranch(List<Version> versions, Branch branch)
    {
        return versions.stream()
            .filter(version -> version.major() == branch.major() && version.minor() == branch.minor())
            .max(Comparator.naturalOrder()).orElseThrow(() -> new IllegalStateException(
                String.format("Failed to find a LibreOffice version for branch [%s]", branch)));
    }

    private static List<Version> extractLibreOfficeVersions(String html)
    {
        Matcher matcher = LIBREOFFICE_VERSION_PATTERN.matcher(html);
        List<Version> versions = new java.util.ArrayList<>();

        while (matcher.find()) {
            versions.add(new Version(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))));
        }

        return versions.stream().distinct().sorted().toList();
    }

    private record Branch(int major, int minor) implements Comparable<Branch>
    {
        @Override
        public int compareTo(Branch other)
        {
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            return Integer.compare(this.minor, other.minor);
        }

        @Override
        public String toString()
        {
            return String.valueOf(this.major) + '.' + String.valueOf(this.minor);
        }
    }

    private record Version(int major, int minor, int patch) implements Comparable<Version>
    {
        @Override
        public int compareTo(Version other)
        {
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }

            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }

            return Integer.compare(this.patch, other.patch);
        }

        @Override
        public String toString()
        {
            return String.valueOf(this.major) + '.' + String.valueOf(this.minor) + '.' + String.valueOf(this.patch);
        }
    }
}
