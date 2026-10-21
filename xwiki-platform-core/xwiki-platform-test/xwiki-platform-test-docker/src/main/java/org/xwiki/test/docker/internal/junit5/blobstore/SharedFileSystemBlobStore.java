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
package org.xwiki.test.docker.internal.junit5.blobstore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.xwiki.test.docker.junit5.TestConfiguration;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;

/**
 * The filesystem blob store shared by the XWiki instances of a cluster: like the database, the data stored in the blob
 * store (attachments, deleted documents and attachments, etc.) must be shared by the members of a cluster, otherwise
 * an instance cannot read what another one stored.
 * <p>
 * The instances running in containers share a Docker volume, which lives on the Docker daemon and can thus be shared
 * even when the tests themselves run in a container. The volume is labelled with the Testcontainers session, so that
 * it's removed with the containers of the session at the end of the tests (it cannot be removed before since the
 * containers using it are kept until then). The instances running on the host share a directory.
 *
 * @version $Id$
 * @since 18.10.0RC1
 * @since 18.4.8
 */
public final class SharedFileSystemBlobStore
{
    /**
     * The location of the filesystem blob store used by XWiki, relative to its permanent directory.
     */
    public static final String STORE_PATH = "store/file";

    /**
     * The names of the volumes created for the running clusters, by test configuration.
     */
    private static final Map<TestConfiguration, String> VOLUME_NAMES =
        Collections.synchronizedMap(new IdentityHashMap<>());

    private SharedFileSystemBlobStore()
    {
        // Utility class
    }

    /**
     * Create the Docker volume shared by the instances running in containers.
     *
     * @param testConfiguration the configuration of the test
     * @return the name of the created volume
     */
    public static String createVolume(TestConfiguration testConfiguration)
    {
        String volumeName = newVolumeName();
        DockerClientFactory.instance().client().createVolumeCmd()
            .withName(volumeName)
            .withLabels(getVolumeLabels())
            .exec();
        VOLUME_NAMES.put(testConfiguration, volumeName);

        return volumeName;
    }

    /**
     * Forget the volume of a cluster which is stopped (the volume itself is removed by Testcontainers at the end of the
     * session).
     *
     * @param testConfiguration the configuration of the test
     */
    public static void forgetVolume(TestConfiguration testConfiguration)
    {
        VOLUME_NAMES.remove(testConfiguration);
    }

    /**
     * @param testConfiguration the configuration of the test
     * @return the name of the Docker volume shared by the instances running in containers
     */
    public static String getVolumeName(TestConfiguration testConfiguration)
    {
        return VOLUME_NAMES.get(testConfiguration);
    }

    /**
     * @return a new volume name, unique for the cluster and the Testcontainers session (so that parallel builds on the
     *     same Docker daemon, or the next cluster of the same tests, don't share it)
     */
    static String newVolumeName()
    {
        return String.format("xwiki-blobstore-%s-%s", DockerClientFactory.SESSION_ID, UUID.randomUUID());
    }

    /**
     * @return the labels of the volume: the ones of the resources of the Testcontainers session, so that the volume is
     *     removed with them
     */
    static Map<String, String> getVolumeLabels()
    {
        Map<String, String> labels = new HashMap<>(DockerClientFactory.DEFAULT_LABELS);
        labels.put(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL, DockerClientFactory.SESSION_ID);

        return labels;
    }

    /**
     * @param testConfiguration the configuration of the test
     * @return the directory shared by the instances running on the host
     */
    public static File getDirectory(TestConfiguration testConfiguration)
    {
        return new File(testConfiguration.getOutputDirectory(), "blobstore");
    }

    /**
     * Mount the shared Docker volume on the location of the filesystem blob store in the container of an instance.
     *
     * @param container the container of the XWiki instance
     * @param testConfiguration the configuration of the test
     */
    public static void mountVolume(GenericContainer<?> container, TestConfiguration testConfiguration)
    {
        Mount mount = new Mount()
            .withType(MountType.VOLUME)
            .withSource(getVolumeName(testConfiguration))
            .withTarget(String.format("%s/%s", testConfiguration.getServletEngine().getPermanentDirectory(),
                STORE_PATH));
        container.withCreateContainerCmdModifier(cmd -> {
            HostConfig hostConfig = cmd.getHostConfig();
            List<Mount> mounts = new ArrayList<>();
            if (hostConfig.getMounts() != null) {
                mounts.addAll(hostConfig.getMounts());
            }
            mounts.add(mount);
            hostConfig.withMounts(mounts);
        });
    }
}
