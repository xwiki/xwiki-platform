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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * Pull the image only every specified milliseconds. This can be used to avoid the pull rate limit issue on dockerhub.
 *
 * @version $Id$
 * @since 13.2RC1
 */
public class DurationImagePullPolicy implements ImagePullPolicy
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DurationImagePullPolicy.class);

    private static final Path PATH = Paths.get(System.getProperty("user.home"), ".xwiki", "pulledImageCache");

    private static Map<String, Long> durationCache;

    private long duration;

    private Path path;

    /**
     * @param duration the duration in ms after which the image should be pulled again
     */
    public DurationImagePullPolicy(long duration)
    {
        this.duration = duration;
        setPath(PATH);
    }

    /**
     * @param path the path to the cache file in which we persist the last pulled date data in
     */
    public void setPath(Path path)
    {
        this.path = path;
    }

    @Override
    public boolean shouldPull(DockerImageName dockerImageName)
    {
        loadPersistedData();

        boolean shouldPull = true;
        String dockerImage = dockerImageName.asCanonicalNameString();
        if (durationCache.containsKey(dockerImage)) {
            // Only pull if the time difference is greater than the duration passed.
            long lastPullDate = durationCache.get(dockerImage);
            long imageAge = System.currentTimeMillis() - lastPullDate;
            if (imageAge <= this.duration) {
                shouldPull = false;
            }
        }

        if (shouldPull) {
            // Add to the cache
            durationCache.put(dockerImage, System.currentTimeMillis());

            // Persist the cache. Ideally we would only persist it once but we don't have any extension point for that
            // except the JVM shutdown hook but that's not guaranteed and writing the file is not too costly to do it
            // every time we have an image pull.
            writeFile();
        }

        return shouldPull;
    }

    private void loadPersistedData()
    {
        if (durationCache == null) {
            durationCache = new HashMap<>();
            synchronized (durationCache) {
                // Read the persisted data from the FS.
                if (Files.exists(this.path)) {
                    readFile();
                }
            }
        }
    }

    private void readFile()
    {
        try (Stream<String> stream = Files.lines(this.path)) {
            stream.forEach(line -> {
                // Format is: <image name>|<date of last pull in long>
                String[] parts = line.split("\\|");
                durationCache.put(parts[0], Long.valueOf(parts[1]));
            });
        } catch (Exception e) {
            LOGGER.error("Failed to read persistence data for DurationImagePullPolicy", e);
        }
    }

    private void writeFile()
    {
        try {
            // Make sure the directories exist
            Files.createDirectories(this.path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(this.path)) {
                durationCache.entrySet().stream().forEach(entry ->
                    writeToFile(writer, entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write persistence data for DurationImagePullPolicy", e);
        }
    }

    private void writeToFile(BufferedWriter writer, String key, Long value)
    {
        try {
            writer.write(String.format("%s|%d\n", key, value));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
