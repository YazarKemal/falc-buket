/**
 * Generic reading contract shared by every future interpretation family.
 *
 * Design goals:
 * - One typed submission boundary instead of 28 unrelated callables.
 * - Domain types only: no image bytes/base64/Android types ever live here.
 * - Family-level engines are shared; per-type differences are configuration.
 *
 * This module is intentionally side-effect free so it can be unit tested
 * without Firebase or OpenAI.
 */

export const READING_FAMILIES = [
  "VISION",
  "CARDS",
  "DREAM_TEXT",
  "ASTROLOGY_CALCULATED",
  "COMPATIBILITY",
  "SYMBOL_CAST",
  "QUESTION_INTUITION"
] as const;

export type ReadingFamily = (typeof READING_FAMILIES)[number];

/** Stable ASCII ids; must match the Android FortuneType.id values exactly. */
export const FORTUNE_TYPE_IDS = [
  // VISION
  "coffee",
  "palm",
  "tea_leaf",
  "candle_wax",
  // CARDS
  "tarot",
  "katina",
  "playing_cards",
  "lenormand",
  "oracle",
  // DREAM/TEXT
  "dream",
  // ASTROLOGY_CALCULATED
  "birth_chart",
  "daily_horoscope",
  "numerology",
  "chinese_zodiac",
  "moon_reading",
  "biorhythm",
  // COMPATIBILITY
  "compatibility",
  // SYMBOL_CAST
  "runes",
  "i_ching",
  "pendulum",
  "daisy",
  "dice",
  // QUESTION_INTUITION
  "love",
  "career_money",
  "crystal_ball",
  "clairvoyance",
  "aura",
  "chakra"
] as const;

export type FortuneTypeId = (typeof FORTUNE_TYPE_IDS)[number];

export const READING_SCHEMA_VERSION = 1;

export interface CardSelection {
  position: number;
  /** Known deck identity when the client selected a real card. */
  cardId?: string;
  reversed?: boolean;
}

export interface SymbolSelection {
  symbolId: string;
  position: number;
}

export interface PlaceData {
  label?: string;
  latitude?: number;
  longitude?: number;
}

export interface PersonData {
  name?: string;
  /** ISO calendar date (YYYY-MM-DD). Localized UI text must be normalized first. */
  birthDate: string;
  /** Local HH:mm. Absence means the birth time is unknown. */
  birthTime?: string;
  /** Validated IANA identifier, e.g. "Europe/Istanbul". */
  timeZone?: string;
  place?: PlaceData;
}

export interface ImagePayloadDescriptor {
  assetId: string;
  role: "primary" | "cup" | "saucer";
  mimeType: string;
  byteCount: number;
  width?: number;
  height?: number;
  reference: {
    kind: "localAttachment" | "privateObject";
    id: string;
  };
}

export interface AdditionalInputs {
  dreamText?: string;
  deckId?: string;
  deckVersion?: string;
  spreadId?: string;
  castMethod?: string;
  targetDate?: string;
}

/**
 * Generic submission envelope. The Android side builds the matching typed
 * domain object; the wire transport is produced by a separate mapper.
 */
export interface ReadingRequest {
  schemaVersion: typeof READING_SCHEMA_VERSION;
  /** Stable across retries so a duplicate submit cannot double-charge. */
  requestId: string;
  readingType: FortuneTypeId;
  question?: string;
  selectedCards?: CardSelection[];
  selectedSymbols?: SymbolSelection[];
  birthData?: PersonData;
  secondPersonData?: PersonData;
  imagePayload?: ImagePayloadDescriptor[];
  additionalInputs?: AdditionalInputs;
}

export type MemoryPolicy = "none" | "context-only" | "read-write";
export type HistoryPolicy = "none" | "history-only" | "history-and-ledger";

export interface ReadingTypeDefinition {
  id: FortuneTypeId;
  family: ReadingFamily;
  /** Real analysis stays disabled until a family engine is implemented. */
  enabled: boolean;
  /** Legacy callable that already serves this type (coffee only for now). */
  legacyEndpoint?: string;
  inputSchemaVersion: number;
  promptVersion: string;
  memoryPolicy: MemoryPolicy;
  historyPolicy: HistoryPolicy;
}

export interface ReadingSection {
  heading: string;
  body: string;
}

export interface ReadingResult {
  schemaVersion: typeof READING_SCHEMA_VERSION;
  title: string;
  summary: string;
  sections: ReadingSection[];
  highlight?: string;
}

export interface ReadingResponse {
  schemaVersion: typeof READING_SCHEMA_VERSION;
  requestId: string;
  readingId: string;
  readingType: FortuneTypeId;
  status: "completed";
  result: ReadingResult;
}

/** Provenance is required so generated claims never become "user facts". */
export interface ProvenancedMemoryCandidate {
  category: string;
  subject: string;
  fact: string;
  importance: number;
  confidence: number;
  source: "user-stated" | "calculated" | "assistant-generated";
}

export interface ReadingContext {
  uid: string;
  requestId: string;
  definition: ReadingTypeDefinition;
  memoryBlock: string;
}

export interface EngineOutput {
  result: ReadingResult;
  memoryCandidates: ProvenancedMemoryCandidate[];
}

export interface ReadingEngine {
  interpret(request: ReadingRequest, context: ReadingContext): Promise<EngineOutput>;
}

export type ReadingContractErrorCode =
  | "INVALID_REQUEST"
  | "UNKNOWN_READING_TYPE"
  | "UNSUPPORTED_SCHEMA_VERSION"
  | "NOT_IMPLEMENTED"
  | "NO_ENGINE";

export class ReadingContractError extends Error {
  constructor(readonly code: ReadingContractErrorCode, message: string) {
    super(`${code}: ${message}`);
    this.name = "ReadingContractError";
  }
}
