import { expect, test } from "vitest";
import { DefaultPageData } from "../../src/components/DefaultPageData";

// Dump test to have at least one test result to show in the CI.
test("DefaultPageData", () => {
  const defaultPageData = new DefaultPageData(
    "id",
    "name",
    "source",
    "xwiki2/1",
  );

  expect(defaultPageData.name).toBe("name");
});
