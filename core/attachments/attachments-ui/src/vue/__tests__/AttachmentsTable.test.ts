/*
 * See the LICENSE file distributed with this work for additional
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
import AttachmentsTable from "../AttachmentsTable.vue";
import { shallowMount } from "@vue/test-utils";
import { CristalApp } from "@xwiki/cristal-api";
import { Attachment } from "@xwiki/cristal-attachments-api";
import { mockI18n } from "@xwiki/cristal-dev-test-utils";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
} from "@xwiki/cristal-model-api";
import { ClickListener } from "@xwiki/cristal-model-click-listener";
import {
  ModelReferenceParser,
  ModelReferenceParserProvider,
} from "@xwiki/cristal-model-reference-api";
import { Container } from "inversify";
import { describe, expect, it, vi } from "vitest";
import { mock } from "vitest-mock-extended";

function initializeMocks() {
  const cristalMock = mock<CristalApp>();
  const containerMock = mock<Container>();
  containerMock.get
    .calledWith("ClickListener")
    .mockReturnValue(mock<ClickListener>());
  const modelReferenceParserProviderMock = mock<ModelReferenceParserProvider>();
  const modelReferenceParserMock = mock<ModelReferenceParser>();
  modelReferenceParserMock.parse
    .calledWith("test.jpg", EntityType.ATTACHMENT)
    .mockReturnValue(
      new AttachmentReference("test.jpg", new DocumentReference("A")),
    );
  modelReferenceParserProviderMock.get.mockReturnValue(
    modelReferenceParserMock,
  );
  containerMock.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(modelReferenceParserProviderMock);
  cristalMock.getContainer.mockReturnValue(containerMock);
  return cristalMock;
}

function shallowMountAttachmentsTable({
  attachments,
  errorMessage,
  isLoading,
}: {
  attachments: Attachment[];
  errorMessage?: string;
  isLoading: boolean;
}) {
  vi.mock("vue-router");
  vi.mock("vue-i18n");
  mockI18n();
  const cristalMock = initializeMocks();
  return shallowMount(AttachmentsTable, {
    props: {
      attachments,
      errorMessage,
      isLoading,
    },
    global: {
      provide: {
        cristal: cristalMock,
      },
    },
  });
}

describe("attachments-table", () => {
  it("Display a loading message", () => {
    const attachmentsTable = shallowMountAttachmentsTable({
      attachments: [],
      errorMessage: undefined,
      isLoading: true,
    });

    expect(attachmentsTable.text()).toEqual("attachments.tab.loading");
  });

  it("Display an error message", () => {
    const attachmentsTable = shallowMountAttachmentsTable({
      attachments: [],
      errorMessage: "error message",
      isLoading: false,
    });

    expect(attachmentsTable.text()).toEqual("error message");
  });

  it("Display an empty list", () => {
    const attachmentsTable = shallowMountAttachmentsTable({
      attachments: [],
      errorMessage: undefined,
      isLoading: false,
    });

    expect(attachmentsTable.text()).toEqual("attachments.tab.noAttachments");
  });

  // eslint-disable-next-line max-statements
  it("Display a list", () => {
    const date = new Date();
    const attachmentsTable = shallowMountAttachmentsTable({
      attachments: [
        {
          size: 1000,
          name: "test.jpg",
          href: "http://localhost/test.jpg",
          mimetype: "application/pdf",
          date: date,
          author: {
            name: "UserName",
          },
          id: "A@test.jpg",
        },
      ],
      errorMessage: undefined,
      isLoading: false,
    });

    const profileLink = attachmentsTable.find("tbody td:nth-child(1) a");
    expect(profileLink.attributes("href")).toBe("http://localhost/test.jpg");
    expect(profileLink.text()).toBe("test.jpg");
    const mimeType = attachmentsTable.find("tbody td:nth-child(2)");
    expect(mimeType.text()).toBe(
      "attachments.tab.table.header.mimetype application/pdf",
    );
    const fileSize = attachmentsTable.find(
      "tbody td:nth-child(3) file-size-stub",
    );
    expect(fileSize.attributes("size")).toBe("1000");
    const dateCell = attachmentsTable.find("tbody td:nth-child(4) c-date-stub");
    const attributes: string = dateCell.attributes("date") ?? "";
    expect(new Date(attributes).getTime()).toBe(date.setMilliseconds(0));
  });
});
