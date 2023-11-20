import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    reporters: ["junit"],
    outputFile: "unit-tests.xml",
  },
});
