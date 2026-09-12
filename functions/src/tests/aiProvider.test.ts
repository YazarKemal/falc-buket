import { test } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import OpenAI from "openai";
import {
  AI_BASE_URL,
  AI_MAX_RETRIES,
  CHAT_CLIENT_TIMEOUT_SECONDS,
  CHAT_FUNCTION_TIMEOUT_SECONDS,
  MEMORY_REQUEST_TIMEOUT_MS,
  TEXT_MODEL,
  TEXT_REQUEST_TIMEOUT_MS,
  VISION_CLIENT_TIMEOUT_SECONDS,
  VISION_FUNCTION_TIMEOUT_SECONDS,
  VISION_MODEL,
  VISION_REQUEST_TIMEOUT_MS
} from "../config/aiModels";
import { ZAI_API_KEY, createZaiClient } from "../ai/zaiClient";
import { ZaiProvider, buildVisionContent, extractCompletionText } from "../ai/zaiProvider";
import { mapProviderError, safeErrorInfo, classifyProviderError, MissingAiSecretError } from "../ai/aiErrors";
import {
  parseJsonObjectFromText,
  assertCoffeeEnvelope,
  assertMemoryEnvelope
} from "../schemas";

// ---- configuration + secret ------------------------------------------------

test("Z.AI base URL and models are centralized and correct", () => {
  assert.equal(AI_BASE_URL, "https://api.z.ai/api/paas/v4/");
  assert.equal(TEXT_MODEL, "glm-5.1");
  assert.equal(VISION_MODEL, "glm-5v-turbo");
});

test("ZAI_API_KEY secret is declared with the correct name", () => {
  assert.equal(ZAI_API_KEY.name, "ZAI_API_KEY");
});

test("provider retries are disabled (one submission -> at most one provider attempt)", () => {
  assert.equal(AI_MAX_RETRIES, 0);
});

test("vision timeout hierarchy is provider < function < client", () => {
  assert.ok(VISION_REQUEST_TIMEOUT_MS < VISION_FUNCTION_TIMEOUT_SECONDS * 1000, "provider < function");
  assert.ok(
    VISION_FUNCTION_TIMEOUT_SECONDS * 1000 < VISION_CLIENT_TIMEOUT_SECONDS * 1000,
    "function < client"
  );
});

test("chat timeout hierarchy is provider < function < client", () => {
  const providerBudget = TEXT_REQUEST_TIMEOUT_MS + MEMORY_REQUEST_TIMEOUT_MS;
  assert.ok(providerBudget < CHAT_FUNCTION_TIMEOUT_SECONDS * 1000, "provider < function");
  assert.ok(CHAT_FUNCTION_TIMEOUT_SECONDS * 1000 < CHAT_CLIENT_TIMEOUT_SECONDS * 1000, "function < client");
});

test("client timeout constants match the Android values", () => {
  // Android BackendConfigTest pins the same literals (cross-repo drift guard).
  assert.equal(VISION_CLIENT_TIMEOUT_SECONDS, 210);
  assert.equal(CHAT_CLIENT_TIMEOUT_SECONDS, 150);
});

// ---- vision content --------------------------------------------------------

test("buildVisionContent keeps cup and saucer independent and text last", () => {
  const content = buildVisionContent(
    ["data:image/jpeg;base64,AAA", "data:image/png;base64,BBB"],
    "yorumla"
  );
  assert.equal(content.length, 3);
  assert.deepEqual(content[0], { type: "image_url", image_url: { url: "data:image/jpeg;base64,AAA" } });
  assert.deepEqual(content[1], { type: "image_url", image_url: { url: "data:image/png;base64,BBB" } });
  assert.deepEqual(content[2], { type: "text", text: "yorumla" });
});

// ---- completion extraction -------------------------------------------------

const okCompletion = (content: unknown, finish: string | null = "stop") => ({
  choices: [{ message: { content }, finish_reason: finish }]
}) as never;

test("extractCompletionText returns trimmed content", () => {
  assert.equal(extractCompletionText(okCompletion("  merhaba  ")), "merhaba");
});

test("extractCompletionText rejects empty, null and truncated content", () => {
  assert.throws(() => extractCompletionText(okCompletion("")), /AI_EMPTY_RESPONSE/);
  assert.throws(() => extractCompletionText(okCompletion(null)), /AI_EMPTY_RESPONSE/);
  assert.throws(() => extractCompletionText(okCompletion("kesik", "length")), /AI_TRUNCATED_RESPONSE/);
});

// ---- provider request construction ----------------------------------------

interface Capture {
  body?: Record<string, unknown>;
  options?: Record<string, unknown>;
}

function fakeProvider(response: unknown, capture: Capture): ZaiProvider {
  const client = {
    chat: {
      completions: {
        create: async (body: Record<string, unknown>, options: Record<string, unknown>) => {
          capture.body = body;
          capture.options = options;
          return response;
        }
      }
    }
  } as unknown as OpenAI;
  return new ZaiProvider(() => client);
}

test("generateText sends text model, system+history order and no vision fields", async () => {
  const capture: Capture = {};
  const provider = fakeProvider(okCompletion("cevap"), capture);
  const text = await provider.generateText({
    system: "SISTEM",
    turns: [
      { role: "user", content: "ilk" },
      { role: "assistant", content: "yanıt" },
      { role: "user", content: "şimdi" }
    ],
    maxTokens: 900
  });
  assert.equal(text, "cevap");
  assert.equal(capture.body?.model, TEXT_MODEL);
  assert.equal(capture.body?.max_tokens, 900);
  assert.equal(capture.body?.response_format, undefined);
  assert.equal(capture.options?.timeout, TEXT_REQUEST_TIMEOUT_MS);
  assert.equal(capture.options?.maxRetries, 0);
  const messages = capture.body?.messages as Array<Record<string, unknown>>;
  assert.deepEqual(messages.map((m) => m.role), ["system", "user", "assistant", "user"]);
  assert.deepEqual(messages.map((m) => m.content), ["SISTEM", "ilk", "yanıt", "şimdi"]);
});

test("generateText requests json_object mode when json is true", async () => {
  const capture: Capture = {};
  const provider = fakeProvider(okCompletion('{"memories":[]}'), capture);
  await provider.generateText({
    system: "s",
    turns: [{ role: "user", content: "u" }],
    maxTokens: 500,
    json: true
  });
  assert.deepEqual(capture.body?.response_format, { type: "json_object" });
});

test("analyzeImages sends vision model with image blocks and no response_format", async () => {
  const capture: Capture = {};
  const provider = fakeProvider(okCompletion("{}"), capture);
  await provider.analyzeImages({
    system: "SISTEM",
    prompt: "fincanı yorumla",
    images: ["data:image/jpeg;base64,AAA", "data:image/png;base64,BBB"],
    maxTokens: 2200
  });
  assert.equal(capture.body?.model, VISION_MODEL);
  assert.equal(capture.body?.max_tokens, 2200);
  assert.equal(capture.body?.response_format, undefined);
  assert.equal(capture.options?.timeout, VISION_REQUEST_TIMEOUT_MS);
  const messages = capture.body?.messages as Array<Record<string, unknown>>;
  const userContent = messages[1].content as Array<Record<string, unknown>>;
  assert.equal(userContent.length, 3);
  assert.equal(userContent[0].type, "image_url");
  assert.equal(userContent[1].type, "image_url");
  assert.equal(userContent[2].type, "text");
});

// ---- JSON parsing ----------------------------------------------------------

test("parseJsonObjectFromText parses plain and fenced JSON", () => {
  assert.deepEqual(parseJsonObjectFromText('{"a":1}'), { a: 1 });
  assert.deepEqual(parseJsonObjectFromText('```json\n{"a":1}\n```'), { a: 1 });
});

test("parseJsonObjectFromText extracts one object from surrounding prose", () => {
  assert.deepEqual(parseJsonObjectFromText('İşte: {"a":"b { c }"} umarım'), { a: "b { c }" });
  assert.deepEqual(parseJsonObjectFromText('{"a":"say \\"hi\\""}'), { a: 'say "hi"' });
});

test("parseJsonObjectFromText rejects invalid roots and ambiguity", () => {
  assert.throws(() => parseJsonObjectFromText("[1,2]"), /INVALID_AI_RESPONSE/);
  assert.throws(() => parseJsonObjectFromText("null"), /INVALID_AI_RESPONSE/);
  assert.throws(() => parseJsonObjectFromText('{"a":'), /INVALID_AI_RESPONSE/);
  assert.throws(() => parseJsonObjectFromText('{"a":1} {"b":2}'), /INVALID_AI_RESPONSE/);
  assert.throws(() => parseJsonObjectFromText('prose {"a":1} more {"b":2}'), /INVALID_AI_RESPONSE/);
});

// ---- structural envelopes --------------------------------------------------

const validCoffee = {
  title: "t",
  summary: "s",
  visualObservations: [{ observation: "o", interpretation: "i" }],
  generalEnergy: "g",
  love: "l",
  careerMoney: "c",
  nearFuture: "n",
  highlight: "h",
  timeWindows: [{ topic: "x", window: "y" }],
  memoryCandidates: []
};

test("assertCoffeeEnvelope accepts valid and empty arrays", () => {
  assertCoffeeEnvelope(validCoffee);
  assertCoffeeEnvelope({ ...validCoffee, visualObservations: [], timeWindows: [], memoryCandidates: [] });
});

test("assertCoffeeEnvelope rejects missing or malformed arrays", () => {
  const { visualObservations: _drop, ...missing } = validCoffee;
  assert.throws(() => assertCoffeeEnvelope(missing), /visualObservations/);
  assert.throws(() => assertCoffeeEnvelope({ ...validCoffee, timeWindows: "nope" }), /timeWindows/);
  assert.throws(
    () => assertCoffeeEnvelope({ ...validCoffee, visualObservations: [{ interpretation: "i" }] }),
    /visualObservations/
  );
});

test("assertMemoryEnvelope requires a memories array", () => {
  assertMemoryEnvelope({ memories: [] });
  assert.throws(() => assertMemoryEnvelope({}), /memories/);
  assert.throws(() => assertMemoryEnvelope({ memories: {} }), /memories/);
});

// ---- error mapping ---------------------------------------------------------

function silenced<T>(fn: () => T): T {
  const original = console.error;
  console.error = () => undefined;
  try {
    return fn();
  } finally {
    console.error = original;
  }
}

test("mapProviderError fails safe for non-SDK errors", () => {
  silenced(() => {
    // Spoofed name/status on a plain Error must NOT be trusted; fail safe to internal.
    assert.equal(mapProviderError(Object.assign(new Error("busy"), { status: 429 }), "op").code, "internal");
    assert.equal(mapProviderError(Object.assign(new Error("boom"), { status: 503 }), "op").code, "internal");
    assert.equal(
      mapProviderError(Object.assign(new Error("slow"), { name: "APIConnectionTimeoutError" }), "op").code,
      "internal"
    );
    assert.equal(mapProviderError(new Error("tamamen bilinmeyen"), "op").code, "internal");
  });
});

test("classifyProviderError uses SDK identity, not name/code", () => {
  const timeoutErr = new OpenAI.APIConnectionTimeoutError();
  const connErr = new OpenAI.APIConnectionError({});
  const rateErr = new OpenAI.RateLimitError(429, undefined, "rate", undefined as never);
  const serverErr = new OpenAI.InternalServerError(503, undefined, "server", undefined as never);
  const authErr = new OpenAI.AuthenticationError(401, undefined, "auth", undefined as never);

  // The SDK classes do not set `name`; identity checks must still work.
  assert.equal(timeoutErr.name, "Error");
  assert.equal(classifyProviderError(timeoutErr).category, "PROVIDER_TIMEOUT");
  assert.equal(classifyProviderError(connErr).category, "PROVIDER_CONNECTION");
  assert.equal(classifyProviderError(rateErr).category, "PROVIDER_RATE_LIMIT");
  assert.equal(classifyProviderError(serverErr).category, "PROVIDER_SERVER");
  assert.equal(classifyProviderError(authErr).category, "PROVIDER_AUTH");
  assert.equal(classifyProviderError(new Error("plain")).category, "PROVIDER_UNKNOWN");
});

test("mapProviderError maps SDK errors to safe HttpsError codes", () => {
  silenced(() => {
    assert.equal(
      mapProviderError(new OpenAI.APIConnectionTimeoutError(), "op").code,
      "deadline-exceeded"
    );
    assert.equal(
      mapProviderError(new OpenAI.APIConnectionError({}), "op").code,
      "unavailable"
    );
    assert.equal(
      mapProviderError(new OpenAI.RateLimitError(429, undefined, "rate", undefined as never), "op").code,
      "resource-exhausted"
    );
    assert.equal(
      mapProviderError(new OpenAI.InternalServerError(503, undefined, "server", undefined as never), "op").code,
      "unavailable"
    );
    assert.equal(
      mapProviderError(new OpenAI.AuthenticationError(401, undefined, "auth", undefined as never), "op").code,
      "internal"
    );
    assert.equal(mapProviderError(new Error("plain"), "op").code, "internal");
  });
});

test("mapProviderError never leaks the provider message", () => {
  silenced(() => {
    const mapped = mapProviderError(Object.assign(new Error("SECRET-LEAK-CANARY"), { status: 500 }), "op");
    assert.ok(!mapped.message.includes("SECRET-LEAK-CANARY"));
  });
});

test("missing ZAI_API_KEY fails safely and maps to internal", () => {
  let caught: unknown;
  try {
    createZaiClient();
  } catch (err) {
    caught = err;
  }
  assert.ok(caught instanceof MissingAiSecretError);
  const mapped = silenced(() => mapProviderError(caught, "op"));
  assert.equal(mapped.code, "internal");
  assert.ok(!mapped.message.includes("ZAI_API_KEY"));
});

test("safeErrorInfo exposes only allowlisted fields", () => {
  const info = safeErrorInfo(Object.assign(new Error("secret"), { status: 429, name: "APIError", code: "X" }));
  assert.deepEqual(info, { category: "PROVIDER_UNKNOWN" });
  assert.ok(!JSON.stringify(info).includes("secret"));

  const sdkInfo = safeErrorInfo(new OpenAI.RateLimitError(429, undefined, "rate", undefined as never));
  assert.deepEqual(sdkInfo, { category: "PROVIDER_RATE_LIMIT", httpStatus: 429 });
});

// ---- source audit ----------------------------------------------------------

function collectProductionSource(dir: string, out: string[] = []): string[] {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === "tests") continue;
      collectProductionSource(full, out);
    } else if (entry.name.endsWith(".ts")) {
      out.push(full);
    }
  }
  return out;
}

test("production source has no OpenAI runtime references", () => {
  const srcDir = path.join(__dirname, "..", "..", "src");
  const needleSecret = "OPENAI" + "_API_KEY";
  const needleResponses = "responses" + ".create";
  const needleStore = "store" + ": false";
  const needleOpenAiHost = "api." + "openai.com";
  const hardcodedKey = new RegExp("sk-" + "[A-Za-z0-9]{20,}");
  const offenders: string[] = [];
  for (const file of collectProductionSource(srcDir)) {
    const content = fs.readFileSync(file, "utf8");
    if (
      content.includes(needleSecret) ||
      content.includes(needleResponses) ||
      content.includes(needleStore) ||
      content.includes(needleOpenAiHost) ||
      hardcodedKey.test(content)
    ) {
      offenders.push(path.relative(srcDir, file));
    }
  }
  assert.deepEqual(offenders, []);
});
