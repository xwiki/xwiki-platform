/**
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
import { DefaultAttachmentsService } from "../defaultAttachmentsService";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it } from "vitest";
import { mock, mockReset } from "vitest-mock-extended";
import type { Storage } from "@xwiki/platform-api";
import type { StorageProvider } from "@xwiki/platform-backend-api";

function initServiceWithMocks() {
  const storageProviderMock = mock<StorageProvider>();
  const storageMock = mock<Storage>();

  storageMock.saveAttachments.mockReturnValue(Promise.resolve(undefined));

  storageProviderMock.get.mockReturnValue(storageMock);
  const service = new DefaultAttachmentsService(storageProviderMock);
  return { storageMock, service };
}

describe("defaultAttachmentsService", () => {
  beforeEach(() => {
    // creates a fresh pinia and makes it active
    // so it's automatically picked up by any useStore() call
    // without having to pass it to it: `useStore(pinia)`
    setActivePinia(createPinia());
  });

  const pageReference = "my.page";
  it("attachments are refresh before upload method returns", async () => {
    const { storageMock, service } = initServiceWithMocks();
    await service.refresh(pageReference);
    // Reset the mock to only spy the mocks during the upload call.
    mockReset(storageMock);
    await service.upload(pageReference, []);
    expect(service.isLoading().value).eq(false);

    // Check individiually each method before checking if they are called in the
    // right order, to make it easier to debug in case of failure.
    expect(storageMock.saveAttachments).toHaveBeenCalled();
    expect(storageMock.getAttachments).toHaveBeenCalled();
    expect(storageMock.saveAttachments).toHaveBeenCalledBefore(
      storageMock.getAttachments,
    );
  });
});
