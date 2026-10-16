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
package org.xwiki.store.filesystem.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Access to the blob store holding the temporary attachments, shared by all the cluster members.
 * <p>
 * Each upload is stored as two blobs, {@code content} then {@code metadata}, in a directory
 * {@code /<scope key>/<user key>/<document key>/<file key>/<upload time>-<size>-<kind><random>/}, where:
 * <ul>
 * <li>the scope key is the SHA-256 of the scope identifier of the browser (see
 * {@link TemporaryAttachmentScopeManager}), so that all the uploads of a browser can be deleted at logout,</li>
 * <li>the user key is the SHA-256 of the serialized user reference and of the scope identifier, so that a scope
 * identifier gives no access to the uploads of another user,</li>
 * <li>the document key is the SHA-256 of the serialized document reference and of its locale,</li>
 * <li>the file key is the SHA-256 of the file name,</li>
 * <li>the upload time is in milliseconds since the epoch, zero padded to 13 digits so that the names sort by time; it
 * gives the age of the upload to the expiry without reading anything,</li>
 * <li>the size of the content in bytes and the kind ({@code u} for an upload, counted in the limits, {@code a} for a
 * copy made by {@code temporarilyAttach}, not counted) let the limits be checked from a listing alone.</li>
 * </ul>
 * The keys are hashes, but the metadata blob holds the document reference, the file name and the author in clear. An
 * upload without its {@code metadata} blob is incomplete (interrupted upload) and is ignored until it expires.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = TemporaryAttachmentStore.class)
@Singleton
public class TemporaryAttachmentStore
{
    /**
     * The name of the blob store.
     */
    public static final String STORE_NAME = "store/temporary-attachments";

    /**
     * The name of the blob holding the content of an upload.
     */
    public static final String CONTENT = "content";

    /**
     * The name of the blob holding the metadata of an upload.
     */
    public static final String METADATA = "metadata";

    /**
     * The number of path segments of a content or metadata blob: scope, user, document, file, upload, blob.
     */
    private static final int BLOB_DEPTH = 6;

    /**
     * The number of attempts of a listing that fails, for example because a directory of the {@code filesystem} store
     * was deleted while it was walked.
     */
    private static final int LIST_ATTEMPTS = 3;

    private static final String GUEST = "XWiki.XWikiGuest";

    private static final char SEPARATOR = '\n';

    private static final char NAME_SEPARATOR = '-';

    private static final char KIND_UPLOAD = 'u';

    private static final char KIND_ATTACHED = 'a';

    private static final int RANDOM_LENGTH = 8;

    private static final String METADATA_VERSION = "version";

    private static final String METADATA_DOCUMENT = "document";

    private static final String METADATA_LOCALE = "locale";

    private static final String METADATA_FILENAME = "filename";

    private static final String METADATA_AUTHOR = "author";

    private static final String METADATA_SIZE = "size";

    private static final String METADATA_DATE = "date";

    private static final String CURRENT_METADATA_VERSION = "1";

    /**
     * Orders upload directories from the oldest to the newest.
     */
    private static final Comparator<BlobPath> UPLOAD_ORDER =
        Comparator.comparing(path -> String.valueOf(path.getFileName()));

    @Inject
    private BlobStoreManager blobStoreManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    private volatile BlobStore store;

    /**
     * The metadata of an upload.
     *
     * @param filename the name of the file
     * @param author the user who uploaded the file, {@code null} for guest
     * @param size the size of the content in bytes
     */
    public record Metadata(String filename, DocumentReference author, long size)
    {
    }

    /**
     * The pending uploads of a user in a scope, copies made by {@code temporarilyAttach} excluded.
     *
     * @param count the number of uploads, complete or not
     * @param size the total size of their content in bytes
     */
    public record Usage(int count, long size)
    {
    }

    /**
     * Gets the blob store, created on first use so that a misconfigured blob store does not prevent the components
     * using temporary attachments (for example the download action) from being created.
     *
     * @return the blob store of the temporary attachments
     * @throws BlobStoreException if the blob store cannot be created
     */
    public BlobStore getStore() throws BlobStoreException
    {
        BlobStore result = this.store;
        if (result == null) {
            synchronized (this) {
                result = this.store;
                if (result == null) {
                    result = this.blobStoreManager.getBlobStore(STORE_NAME);
                    this.store = result;
                }
            }
        }

        return result;
    }

    /**
     * @param scopeId the scope identifier of the browser
     * @return the directory holding all the uploads of the browser, whatever the user
     */
    public BlobPath getScopePath(String scopeId)
    {
        return BlobPath.absolute(DigestUtils.sha256Hex(scopeId));
    }

    /**
     * @param scopeId the scope identifier of the browser
     * @param userReference the current user, {@code null} for guest
     * @return the directory holding the uploads of the user in the browser
     */
    public BlobPath getUserPath(String scopeId, DocumentReference userReference)
    {
        String user = userReference != null ? this.serializer.serialize(userReference) : GUEST;

        return getScopePath(scopeId).resolve(BlobPath.relative(DigestUtils.sha256Hex(user + SEPARATOR + scopeId)));
    }

    /**
     * @param userPath the directory returned by {@link #getUserPath(String, DocumentReference)}
     * @param documentReference the document the uploads are for; documents differing only by their locale are
     *     distinct, a {@code null} locale and {@link Locale#ROOT} are the same
     * @return the directory holding the uploads of the user for the document
     */
    public BlobPath getDocumentPath(BlobPath userPath, DocumentReference documentReference)
    {
        Locale locale = documentReference.getLocale();
        String key = this.serializer.serialize(documentReference) + SEPARATOR
            + (locale != null ? locale.toString() : "");

        return userPath.resolve(BlobPath.relative(DigestUtils.sha256Hex(key)));
    }

    /**
     * @param documentPath the directory returned by {@link #getDocumentPath(BlobPath, DocumentReference)}
     * @param fileName the name of the file, not {@code null}
     * @return the directory holding the uploads of the file (each upload of the same name is in its own directory)
     */
    public BlobPath getFilePath(BlobPath documentPath, String fileName)
    {
        return documentPath.resolve(BlobPath.relative(DigestUtils.sha256Hex(fileName)));
    }

    /**
     * Finds the newest complete upload of each file below the given directory.
     *
     * @param prefix a user, document or file directory
     * @return the newest complete upload directory of each file directory found below the prefix
     * @throws BlobStoreException if the listing fails
     */
    public Map<BlobPath, BlobPath> getNewestUploads(BlobPath prefix) throws BlobStoreException
    {
        Map<BlobPath, Set<String>> uploads = listUploads(prefix, false);

        Map<BlobPath, BlobPath> newest = new HashMap<>();
        uploads.forEach((uploadPath, names) -> {
            if (names.contains(CONTENT) && names.contains(METADATA) && getUploadTime(uploadPath).isPresent()) {
                newest.merge(uploadPath.getParent(), uploadPath,
                    (a, b) -> UPLOAD_ORDER.compare(a, b) >= 0 ? a : b);
            }
        });

        return newest;
    }

    /**
     * Gives the time of a new upload of a file: the current time, or just after the newest existing upload of the
     * file if that one is in the future (clock of another member ahead), so that the new upload is always the newest.
     *
     * @param filePath the file directory
     * @param now the current time in milliseconds since the epoch
     * @return the time to use for a new upload of the file
     * @throws BlobStoreException if the listing fails
     */
    public long getNextUploadTime(BlobPath filePath, long now) throws BlobStoreException
    {
        long next = now;
        for (BlobPath uploadPath : listUploads(filePath, false).keySet()) {
            OptionalLong time = getUploadTime(uploadPath);
            if (time.isPresent() && time.getAsLong() >= next) {
                next = time.getAsLong() + 1;
            }
        }

        return next;
    }

    /**
     * Computes the pending uploads of a user in a scope, to enforce the limits of the configuration. The sizes are read
     * from the upload directory names, so this costs one listing.
     *
     * @param userPath the directory returned by {@link #getUserPath(String, DocumentReference)}
     * @param ignoredFilePath a file directory whose uploads are not counted (they are about to be replaced), or
     *     {@code null}
     * @return the number and total size of the uploads, copies made by {@code temporarilyAttach} excluded
     * @throws BlobStoreException if the listing fails
     */
    public Usage getUsage(BlobPath userPath, BlobPath ignoredFilePath) throws BlobStoreException
    {
        int count = 0;
        long size = 0;
        for (BlobPath uploadPath : listUploads(userPath, false).keySet()) {
            if (!uploadPath.getParent().equals(ignoredFilePath) && isCounted(uploadPath)) {
                count++;
                size += getUploadSize(uploadPath).orElse(0);
            }
        }

        return new Usage(count, size);
    }

    /**
     * Stores a new upload: the content first, then the metadata, which marks the upload as complete.
     *
     * @param filePath the directory returned by {@link #getFilePath(BlobPath, String)}
     * @param uploadTime the time of the upload in milliseconds since the epoch
     * @param content the content to store, closed by this method
     * @param documentReference the document the upload is for, kept in the metadata for diagnostics
     * @param metadata the metadata to store
     * @param counted {@code true} for an upload counted in the limits, {@code false} for a copy made by
     *     {@code temporarilyAttach}
     * @return the directory of the new upload
     * @throws BlobStoreException if the upload cannot be stored
     */
    public BlobPath write(BlobPath filePath, long uploadTime, InputStream content,
        DocumentReference documentReference, Metadata metadata, boolean counted) throws BlobStoreException
    {
        BlobPath uploadPath = filePath.resolve(BlobPath.relative("%013d%c%d%c%c%s".formatted(uploadTime,
            NAME_SEPARATOR, Math.max(0, metadata.size()), NAME_SEPARATOR, counted ? KIND_UPLOAD : KIND_ATTACHED,
            RandomStringUtils.secure().nextAlphanumeric(RANDOM_LENGTH))));

        BlobStore blobStore = getStore();
        blobStore.getBlob(uploadPath.resolve(CONTENT)).writeFromStream(content);

        Properties properties = new Properties();
        properties.setProperty(METADATA_VERSION, CURRENT_METADATA_VERSION);
        properties.setProperty(METADATA_DOCUMENT, this.serializer.serialize(documentReference));
        if (documentReference.getLocale() != null) {
            properties.setProperty(METADATA_LOCALE, documentReference.getLocale().toString());
        }
        properties.setProperty(METADATA_FILENAME, metadata.filename());
        if (metadata.author() != null) {
            properties.setProperty(METADATA_AUTHOR, this.serializer.serialize(metadata.author()));
        }
        properties.setProperty(METADATA_SIZE, String.valueOf(metadata.size()));
        properties.setProperty(METADATA_DATE, String.valueOf(uploadTime));

        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException e) {
            // Cannot happen with a StringWriter.
            throw new BlobStoreException("Failed to serialize the temporary attachment metadata", e);
        }
        blobStore.getBlob(uploadPath.resolve(METADATA))
            .writeFromStream(new ByteArrayInputStream(writer.toString().getBytes(UTF_8)));

        return uploadPath;
    }

    /**
     * @param uploadPath an upload directory
     * @return the metadata of the upload
     * @throws BlobStoreException if the metadata cannot be read
     */
    public Metadata readMetadata(BlobPath uploadPath) throws BlobStoreException
    {
        Properties properties = new Properties();
        try (InputStream stream = getStore().getBlob(uploadPath.resolve(METADATA)).getStream()) {
            properties.load(new InputStreamReader(stream, UTF_8));
        } catch (IOException e) {
            throw new BlobStoreException("Failed to read the temporary attachment metadata", e);
        }

        String author = properties.getProperty(METADATA_AUTHOR);
        try {
            return new Metadata(properties.getProperty(METADATA_FILENAME),
                author != null ? this.resolver.resolve(author) : null,
                Long.parseLong(properties.getProperty(METADATA_SIZE, "0")));
        } catch (NumberFormatException e) {
            throw new BlobStoreException("Invalid temporary attachment metadata", e);
        }
    }

    /**
     * @param uploadPath an upload directory
     * @return the blob holding the content of the upload
     * @throws BlobStoreException if the blob cannot be resolved
     */
    public Blob getContent(BlobPath uploadPath) throws BlobStoreException
    {
        return getStore().getBlob(uploadPath.resolve(CONTENT));
    }

    /**
     * Deletes the uploads of a file that are older than the given one (a newer one, uploaded concurrently, is kept).
     * Failures are logged, not thrown: the older uploads are hidden by the new one and expire anyway.
     *
     * @param filePath the file directory
     * @param keptUploadPath the upload to keep
     */
    public void deleteOlderUploads(BlobPath filePath, BlobPath keptUploadPath)
    {
        try {
            for (BlobPath uploadPath : listUploads(filePath, true).keySet()) {
                if (UPLOAD_ORDER.compare(uploadPath, keptUploadPath) < 0) {
                    deleteAll(uploadPath);
                }
            }
        } catch (BlobStoreException e) {
            logFailure("Failed to delete the older uploads of a temporary attachment: [{}]", e);
        }
    }

    /**
     * Checks whether there is anything below the given directory.
     *
     * @param prefix the directory
     * @return {@code true} if there is at least one blob below the directory
     * @throws BlobStoreException if the store cannot be reached
     */
    public boolean exists(BlobPath prefix) throws BlobStoreException
    {
        try {
            return getStore().hasDescendants(prefix);
        } catch (RuntimeException e) {
            throw new BlobStoreException("Failed to check the temporary attachments", e);
        }
    }

    /**
     * Deletes everything below the given directory, if there is anything. Deleting something that another member
     * already deleted is not an error.
     *
     * @param prefix the directory to delete
     * @return {@code true} if there was something to delete
     * @throws BlobStoreException if the store cannot be reached
     */
    public boolean delete(BlobPath prefix) throws BlobStoreException
    {
        boolean existed = exists(prefix);
        if (existed) {
            deleteAll(prefix);
        }

        return existed;
    }

    /**
     * Deletes everything below the given directory without checking first. Failures are logged, not thrown: what is
     * left expires.
     *
     * @param prefix the directory to delete
     * @return {@code true} if the deletion succeeded (or another member deleted the same blobs)
     */
    public boolean deleteAll(BlobPath prefix)
    {
        try {
            getStore().deleteDescendants(prefix);
            return true;
        } catch (BlobStoreException | RuntimeException e) {
            // Another member may be deleting the same blobs (explicit removal or expiry): that is not an error.
            if (isStillPresent(prefix)) {
                logFailure("Failed to delete temporary attachments: [{}]", e);
                return false;
            }
            return true;
        }
    }

    /**
     * Deletes every upload, complete or not, that is older than the given age. A listing interrupted by concurrent
     * deletions is retried, and what was listed before a last failure is still processed: the next run handles the
     * rest.
     *
     * @param now the current time in milliseconds since the epoch
     * @param maxAge the age above which an upload is deleted
     * @return the number of deleted uploads
     * @throws BlobStoreException if the store cannot be reached
     */
    public int deleteExpired(long now, Duration maxAge) throws BlobStoreException
    {
        long limit = now - maxAge.toMillis();
        Set<BlobPath> expired = new HashSet<>();
        for (BlobPath uploadPath : listUploads(BlobPath.root(), true).keySet()) {
            OptionalLong uploadTime = getUploadTime(uploadPath);
            if (uploadTime.isPresent() && uploadTime.getAsLong() < limit) {
                expired.add(uploadPath);
            }
        }

        int count = 0;
        for (BlobPath uploadPath : expired) {
            if (deleteAll(uploadPath)) {
                count++;
            }
        }

        return count;
    }

    /**
     * @param uploadPath an upload directory
     * @return the upload time in milliseconds since the epoch, or empty if the directory name is not an upload name
     */
    public static OptionalLong getUploadTime(BlobPath uploadPath)
    {
        return parseLong(StringUtils.split(String.valueOf(uploadPath.getFileName()), NAME_SEPARATOR), 0);
    }

    /**
     * @param uploadPath an upload directory
     * @return the size of the content in bytes, or empty if the directory name is not an upload name
     */
    public static OptionalLong getUploadSize(BlobPath uploadPath)
    {
        return parseLong(StringUtils.split(String.valueOf(uploadPath.getFileName()), NAME_SEPARATOR), 1);
    }

    private static boolean isCounted(BlobPath uploadPath)
    {
        String[] parts = StringUtils.split(String.valueOf(uploadPath.getFileName()), NAME_SEPARATOR);

        return parts.length < 3 || parts[2].isEmpty() || parts[2].charAt(0) != KIND_ATTACHED;
    }

    private static OptionalLong parseLong(String[] parts, int index)
    {
        if (parts.length < 3 || !StringUtils.isNumeric(parts[index])) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(Long.parseLong(parts[index]));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * Lists the upload directories below a prefix with the names of the blobs found in each of them. Blobs that do not
     * follow the layout of this store (for example a migration marker) are ignored. A listing that fails (for example a
     * directory of the {@code filesystem} store deleted while it is walked) is retried.
     *
     * @param prefix the directory to list
     * @param partial {@code true} to return what was listed before the last failure instead of failing
     */
    private Map<BlobPath, Set<String>> listUploads(BlobPath prefix, boolean partial) throws BlobStoreException
    {
        Map<BlobPath, Set<String>> uploads = new HashMap<>();
        RuntimeException failure = null;
        for (int attempt = 0; attempt < LIST_ATTEMPTS; attempt++) {
            uploads.clear();
            try (Stream<Blob> blobs = getStore().listDescendants(prefix)) {
                blobs.map(Blob::getPath)
                    .filter(path -> path.getNameCount() == BLOB_DEPTH && path.startsWith(prefix))
                    .forEach(path -> uploads.computeIfAbsent(path.getParent(), key -> new HashSet<>())
                        .add(String.valueOf(path.getFileName())));
                return uploads;
            } catch (RuntimeException e) {
                // Some stores report listing failures as runtime exceptions while iterating.
                failure = e;
            }
        }

        if (partial) {
            this.logger.debug("Listing of the temporary attachments interrupted, continuing with [{}] uploads: [{}]",
                uploads.size(), ExceptionUtils.getRootCauseMessage(failure));
            return uploads;
        }

        throw new BlobStoreException("Failed to list the temporary attachments", failure);
    }

    private boolean isStillPresent(BlobPath prefix)
    {
        try {
            return getStore().hasDescendants(prefix);
        } catch (BlobStoreException | RuntimeException e) {
            return true;
        }
    }

    private void logFailure(String message, Exception e)
    {
        this.logger.warn(message, ExceptionUtils.getRootCauseMessage(e));
        this.logger.debug("Full stack trace: ", e);
    }
}
