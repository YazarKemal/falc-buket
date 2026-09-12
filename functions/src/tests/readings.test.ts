import { test } from "node:test";
import assert from "node:assert/strict";
import { FORTUNE_TYPE_IDS, ReadingContractError } from "../readings/contracts";
import {
  READING_TYPE_DEFINITIONS,
  definitionFor,
  familyCounts,
  parseReadingRequest,
  requireDefinition
} from "../readings/typeRegistry";
import { analyzeReading } from "../readings/orchestrator";

test("registry defines all 28 fortune types exactly once", () => {
  assert.equal(READING_TYPE_DEFINITIONS.length, 28);
  assert.equal(FORTUNE_TYPE_IDS.length, 28);
  const ids = READING_TYPE_DEFINITIONS.map((d) => d.id);
  assert.equal(new Set(ids).size, 28);
  for (const id of FORTUNE_TYPE_IDS) {
    assert.ok(ids.includes(id), `missing definition for ${id}`);
  }
});

test("family counts collapse 28 types into 4/5/1/6/1/5/6", () => {
  assert.deepEqual(familyCounts(), {
    VISION: 4,
    CARDS: 5,
    DREAM_TEXT: 1,
    ASTROLOGY_CALCULATED: 6,
    COMPATIBILITY: 1,
    SYMBOL_CAST: 5,
    QUESTION_INTUITION: 6
  });
});

test("coffee keeps its legacy endpoint and all generic types stay disabled", () => {
  assert.equal(definitionFor("coffee")?.legacyEndpoint, "analyzeCoffeeReading");
  assert.equal(definitionFor("coffee")?.family, "VISION");
  for (const d of READING_TYPE_DEFINITIONS) {
    assert.equal(d.enabled, false, `${d.id} should be disabled this milestone`);
  }
});

test("unknown reading type is rejected", () => {
  assert.equal(definitionFor("not_a_type"), undefined);
  assert.throws(() => requireDefinition("not_a_type"), /UNKNOWN_READING_TYPE/);
});

const validRequest = {
  schemaVersion: 1,
  requestId: "req-1",
  readingType: "tarot",
  question: "Yakın geleceğim nasıl?",
  selectedCards: [{ position: 0, cardId: "the-fool" }]
};

test("parseReadingRequest accepts a valid envelope", () => {
  const parsed = parseReadingRequest(validRequest);
  assert.equal(parsed.readingType, "tarot");
  assert.equal(parsed.requestId, "req-1");
});

test("parseReadingRequest rejects non-object", () => {
  assert.throws(() => parseReadingRequest("nope"), /INVALID_REQUEST/);
});

test("parseReadingRequest rejects unsupported schema version", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, schemaVersion: 2 }),
    /UNSUPPORTED_SCHEMA_VERSION/
  );
});

test("parseReadingRequest rejects missing requestId", () => {
  const { requestId: _omit, ...rest } = validRequest;
  assert.throws(() => parseReadingRequest(rest), /INVALID_REQUEST/);
});

test("parseReadingRequest rejects unknown readingType", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, readingType: "made_up" }),
    /UNKNOWN_READING_TYPE/
  );
});

test("parseReadingRequest rejects overlong question and non-array selections", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, question: "x".repeat(4001) }),
    /INVALID_REQUEST/
  );
  assert.throws(
    () => parseReadingRequest({ ...validRequest, selectedCards: "not-an-array" }),
    /INVALID_REQUEST/
  );
});

test("parseReadingRequest rejects oversized lists", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, selectedSymbols: new Array(13).fill({ symbolId: "x", position: 0 }) }),
    /INVALID_REQUEST/
  );
});

test("parseReadingRequest normalizes and drops unknown fields", () => {
  const parsed = parseReadingRequest({ ...validRequest, hackerField: "ignore-me" });
  assert.equal((parsed as unknown as Record<string, unknown>).hackerField, undefined);
  assert.deepEqual(parsed.selectedCards, [{ position: 0, cardId: "the-fool" }]);
});

test("parseReadingRequest rejects birthData without a valid ISO birthDate", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, birthData: {} }),
    /INVALID_REQUEST/
  );
  assert.throws(
    () => parseReadingRequest({ ...validRequest, birthData: { birthDate: "12.03.1990" } }),
    /INVALID_REQUEST/
  );
  const parsed = parseReadingRequest({ ...validRequest, birthData: { birthDate: "1990-03-12" } });
  assert.equal(parsed.birthData?.birthDate, "1990-03-12");
});

test("parseReadingRequest rejects invalid time zone and coordinates", () => {
  assert.throws(
    () => parseReadingRequest({ ...validRequest, birthData: { birthDate: "1990-03-12", timeZone: "Mars/Olympus" } }),
    /INVALID_REQUEST/
  );
  assert.throws(
    () => parseReadingRequest({ ...validRequest, birthData: { birthDate: "1990-03-12", place: { latitude: 200 } } }),
    /INVALID_REQUEST/
  );
});

test("parseReadingRequest validates image descriptors", () => {
  const descriptor = {
    assetId: "cup-1",
    role: "cup",
    mimeType: "image/jpeg",
    byteCount: 1024,
    reference: { kind: "localAttachment", id: "attachment-1" }
  };
  const parsed = parseReadingRequest({ ...validRequest, imagePayload: [descriptor] });
  assert.equal(parsed.imagePayload?.[0].role, "cup");

  assert.throws(
    () => parseReadingRequest({ ...validRequest, imagePayload: [{ ...descriptor, mimeType: "application/pdf" }] }),
    /INVALID_REQUEST/
  );
  assert.throws(
    () => parseReadingRequest({ ...validRequest, imagePayload: [{ ...descriptor, role: "hacker" }] }),
    /INVALID_REQUEST/
  );
});

test("registry policy matches the Android catalog personalization flags", () => {
  const personalized = ["coffee", "tarot", "love", "career_money"];
  for (const d of READING_TYPE_DEFINITIONS) {
    if (personalized.includes(d.id)) {
      assert.equal(d.memoryPolicy, "read-write", `${d.id} memory policy`);
      assert.equal(d.historyPolicy, "history-and-ledger", `${d.id} history policy`);
    } else {
      assert.equal(d.memoryPolicy, "none", `${d.id} memory policy`);
      assert.equal(d.historyPolicy, "none", `${d.id} history policy`);
    }
  }
});

test("analyzeReading fails closed for disabled types", async () => {
  await assert.rejects(
    () => analyzeReading(validRequest, "uid-1"),
    (err: unknown) => err instanceof ReadingContractError && err.code === "NOT_IMPLEMENTED"
  );
});
