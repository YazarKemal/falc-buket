/**
 * Generic reading orchestration skeleton.
 *
 * This is preparation only: no OpenAI call happens here yet and no engine is
 * registered. It exists so that when the first family engine is implemented
 * there is exactly one place that authenticates, validates, dispatches by
 * family and persists. Coffee continues to use its dedicated callable.
 */

import {
  EngineOutput,
  ReadingContractError,
  ReadingEngine,
  ReadingFamily,
  ReadingRequest,
  ReadingResponse,
  READING_SCHEMA_VERSION
} from "./contracts";
import { parseReadingRequest, requireDefinition } from "./typeRegistry";

/** One engine per family; intentionally empty until a family is implemented. */
const ENGINES: Partial<Record<ReadingFamily, ReadingEngine>> = {};

export function registerEngine(family: ReadingFamily, engine: ReadingEngine): void {
  if (ENGINES[family]) {
    throw new Error(`engine already registered for family: ${family}`);
  }
  ENGINES[family] = engine;
}

export function registeredFamilies(): ReadingFamily[] {
  return Object.keys(ENGINES) as ReadingFamily[];
}

/**
 * Validates the request and dispatches to the family engine. Unsupported or
 * not-yet-implemented types fail closed rather than silently returning a mock.
 */
export async function analyzeReading(
  rawRequest: unknown,
  uid: string,
  memoryBlock = ""
): Promise<ReadingResponse> {
  const request = parseReadingRequest(rawRequest);
  const definition = requireDefinition(request.readingType);

  if (!definition.enabled) {
    throw new ReadingContractError(
      "NOT_IMPLEMENTED",
      `${definition.id} henüz genel okuma hattına bağlanmadı`
    );
  }

  const engine = ENGINES[definition.family];
  if (!engine) {
    throw new ReadingContractError(
      "NO_ENGINE",
      `${definition.family} ailesi için motor kayıtlı değil`
    );
  }

  const output: EngineOutput = await engine.interpret(request, {
    uid,
    requestId: request.requestId,
    definition,
    memoryBlock
  });

  return {
    schemaVersion: READING_SCHEMA_VERSION,
    requestId: request.requestId,
    readingId: "",
    readingType: request.readingType,
    status: "completed",
    result: output.result
  };
}
