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
import { PathLinkTargetType } from "../PathLinkTargetType";
import { UNCLinkTargetType } from "../UNCLinkTargetType";
import { describe, expect, it } from "vitest";

describe.each([
  {
    targetType: new PathLinkTargetType(),
    type: "path",
    order: 300,
    label: "Path",
    location: "/xwiki/bin/view/Main/",
  },
  {
    targetType: new UNCLinkTargetType(),
    type: "unc",
    order: 400,
    label: "UNC path",
    location: "\\\\server\\share",
  },
])(
  "$targetType.constructor.name",
  ({ targetType, type, order, label, location }) => {
    it("has the expected identity, between the e-mail and the URL link target types", async () => {
      expect(targetType.type).toBe(type);
      expect(targetType.order).toBe(order);
      await expect(targetType.getLabel("en")).resolves.toBe(label);
    });

    it("doesn't parse URLs, which are handled by the URL link target type", () => {
      expect(targetType.tryParseURL(location)).toBeNull();
    });

    it("uses the location as URL", () => {
      expect(targetType.serializeURL({ location })).toBe(location);
    });

    it("matches only the references of its own resource type", () => {
      ["url", "doc", "unknown", type === "path" ? "unc" : "path"].forEach(
        (otherType) => {
          expect(
            targetType.tryParseReference({
              type: otherType,
              typed: true,
              reference: location,
              parameters: {},
            }),
          ).toBeNull();
        },
      );
      expect(
        targetType.tryParseReference({
          type,
          typed: true,
          reference: location,
          parameters: {},
        }),
      ).toEqual({ location });
    });

    it("converts its config back into a typed reference of its own resource type", () => {
      // Untyped, the location would be parsed back as a document reference.
      expect(targetType.configToReference({ location })).toEqual({
        type,
        typed: true,
        reference: location,
        parameters: {},
      });
      expect(targetType.configToReference({ location: " " })).toBeUndefined();
    });
  },
);
