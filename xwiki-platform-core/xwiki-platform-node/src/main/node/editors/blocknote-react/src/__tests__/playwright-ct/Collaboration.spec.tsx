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
import { CollaborationForTest } from "./Collaboration.story";
import { expect, test } from "@playwright/experimental-ct-react";

test("Editors with different names in the same collaboration session keep their own content", async ({
  mount,
}) => {
  const component = await mount(
    <CollaborationForTest
      editors={[
        { name: "content", text: "Page content" },
        { name: "XWiki.Comment_0_comment", text: "Comment" },
      ]}
    />,
  );

  await expect(
    component.getByTestId("editor-0").locator(".bn-editor"),
  ).toHaveText("Page content");
  await expect(
    component.getByTestId("editor-1").locator(".bn-editor"),
  ).toHaveText("Comment");
});

test("Editors with the same name in the same collaboration session share their content", async ({
  mount,
}) => {
  const component = await mount(
    <CollaborationForTest
      editors={[
        { name: "content", text: "First" },
        { name: "content", text: "Second" },
      ]}
    />,
  );

  // The content is loaded by the first editor joining the session, the other one getting it from the session.
  await expect(
    component.getByTestId("editor-0").locator(".bn-editor"),
  ).toHaveText("First");
  await expect(
    component.getByTestId("editor-1").locator(".bn-editor"),
  ).toHaveText("First");
});

test("An editor without a name doesn't take part in the collaboration session", async ({
  mount,
}) => {
  const component = await mount(
    <CollaborationForTest
      editors={[{ name: "content", text: "Shared" }, { text: "Not shared" }]}
    />,
  );

  await expect(
    component.getByTestId("editor-0").locator(".bn-editor"),
  ).toHaveText("Shared");
  await expect(
    component.getByTestId("editor-1").locator(".bn-editor"),
  ).toHaveText("Not shared");
});
