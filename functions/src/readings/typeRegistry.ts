/**
 * The 28 FortuneType ids collapsed into 7 interpretation families.
 *
 * There is deliberately one definition per type and one engine per family.
 * Real analysis is disabled for every type in this milestone; coffee keeps
 * using its existing dedicated callable until it is migrated behind the
 * generic pipeline in a later, separately verified step.
 */

import {
  AdditionalInputs,
  CardSelection,
  FORTUNE_TYPE_IDS,
  FortuneTypeId,
  ImagePayloadDescriptor,
  PersonData,
  PlaceData,
  ReadingContractError,
  ReadingFamily,
  ReadingRequest,
  ReadingTypeDefinition,
  READING_SCHEMA_VERSION,
  SymbolSelection
} from "./contracts";

/**
 * Types whose Android catalog flags are memoryEnabled=true / historyEnabled=true
 * (see FortuneType.kt). Keep this in sync with the catalog; the backend must not
 * personalize or persist readings the catalog presents as non-personalized.
 */
const CATALOG_MEMORY_HISTORY_ENABLED: ReadonlySet<string> = new Set([
  "coffee",
  "tarot",
  "love",
  "career_money"
]);

export const READING_TYPE_DEFINITIONS: readonly ReadingTypeDefinition[] = [
  // VISION — image input interpreted by a shared vision engine.
  def("coffee", "VISION", "analyzeCoffeeReading"),
  def("palm", "VISION"),
  def("tea_leaf", "VISION"),
  def("candle_wax", "VISION"),

  // CARDS — deck identity + spread semantics.
  def("tarot", "CARDS"),
  def("katina", "CARDS"),
  def("playing_cards", "CARDS"),
  def("lenormand", "CARDS"),
  def("oracle", "CARDS"),

  // DREAM/TEXT — narrative symbol interpretation.
  def("dream", "DREAM_TEXT"),

  // ASTROLOGY_CALCULATED — deterministic calculation then interpretation.
  def("birth_chart", "ASTROLOGY_CALCULATED"),
  def("daily_horoscope", "ASTROLOGY_CALCULATED"),
  def("numerology", "ASTROLOGY_CALCULATED"),
  def("chinese_zodiac", "ASTROLOGY_CALCULATED"),
  def("moon_reading", "ASTROLOGY_CALCULATED"),
  def("biorhythm", "ASTROLOGY_CALCULATED"),

  // COMPATIBILITY — pairwise derived from calculated features.
  def("compatibility", "COMPATIBILITY"),

  // SYMBOL_CAST — reproducible cast outcome then interpretation.
  def("runes", "SYMBOL_CAST"),
  def("i_ching", "SYMBOL_CAST"),
  def("pendulum", "SYMBOL_CAST"),
  def("daisy", "SYMBOL_CAST"),
  def("dice", "SYMBOL_CAST"),

  // QUESTION_INTUITION — free question, shared persona.
  def("love", "QUESTION_INTUITION"),
  def("career_money", "QUESTION_INTUITION"),
  def("crystal_ball", "QUESTION_INTUITION"),
  def("clairvoyance", "QUESTION_INTUITION"),
  def("aura", "QUESTION_INTUITION"),
  def("chakra", "QUESTION_INTUITION")
];

function def(
  id: FortuneTypeId,
  family: ReadingFamily,
  legacyEndpoint?: string
): ReadingTypeDefinition {
  const personalization = CATALOG_MEMORY_HISTORY_ENABLED.has(id);
  return {
    id,
    family,
    enabled: false,
    legacyEndpoint,
    inputSchemaVersion: 1,
    promptVersion: "v1",
    memoryPolicy: personalization ? "read-write" : "none",
    historyPolicy: personalization ? "history-and-ledger" : "none"
  };
}

const BY_ID = new Map<string, ReadingTypeDefinition>(
  READING_TYPE_DEFINITIONS.map((d) => [d.id, d])
);

export function definitionFor(id: string): ReadingTypeDefinition | undefined {
  return BY_ID.get(id);
}

export function requireDefinition(id: string): ReadingTypeDefinition {
  const found = BY_ID.get(id);
  if (!found) {
    throw new ReadingContractError("UNKNOWN_READING_TYPE", `bilinmeyen fal türü: ${id}`);
  }
  return found;
}

export function familyCounts(): Record<ReadingFamily, number> {
  const counts = {} as Record<ReadingFamily, number>;
  for (const d of READING_TYPE_DEFINITIONS) {
    counts[d.family] = (counts[d.family] ?? 0) + 1;
  }
  return counts;
}

const MAX_QUESTION_LENGTH = 4000;
const MAX_DREAM_TEXT_LENGTH = 8000;
const MAX_LIST_ITEMS = 12;
const MAX_ID_LENGTH = 128;
const MAX_NAME_LENGTH = 120;
const MAX_PLACE_LENGTH = 200;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const LOCAL_TIME = /^([01]\d|2[0-3]):[0-5]\d$/;
const IMAGE_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const IMAGE_ROLES = new Set(["primary", "cup", "saucer"]);
const REFERENCE_KINDS = new Set(["localAttachment", "privateObject"]);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function fail(message: string): never {
  throw new ReadingContractError("INVALID_REQUEST", message);
}

function optionalString(
  value: unknown,
  field: string,
  max: number
): string | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value !== "string") fail(`${field} metin değil`);
  const trimmed = value.trim();
  if (trimmed.length === 0) return undefined;
  if (trimmed.length > max) fail(`${field} çok uzun`);
  return trimmed;
}

function requiredString(value: unknown, field: string, max: number): string {
  const parsed = optionalString(value, field, max);
  if (parsed === undefined) fail(`${field} gerekli`);
  return parsed;
}

function optionalBoolean(value: unknown, field: string): boolean | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value !== "boolean") fail(`${field} boolean değil`);
  return value;
}

function optionalInteger(value: unknown, field: string, min: number, max: number): number | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value !== "number" || !Number.isInteger(value) || value < min || value > max) {
    fail(`${field} geçersiz tam sayı`);
  }
  return value;
}

function optionalNumber(value: unknown, field: string, min: number, max: number): number | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value !== "number" || !Number.isFinite(value) || value < min || value > max) {
    fail(`${field} geçersiz sayı`);
  }
  return value;
}

function parseIsoDate(value: unknown, field: string): string {
  const text = requiredString(value, field, 10);
  if (!ISO_DATE.test(text)) fail(`${field} YYYY-MM-DD biçiminde olmalı`);
  const parsed = new Date(`${text}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== text) {
    fail(`${field} geçersiz takvim tarihi`);
  }
  return text;
}

function parseTimeZone(value: unknown, field: string): string | undefined {
  const text = optionalString(value, field, 64);
  if (text === undefined) return undefined;
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: text });
  } catch {
    fail(`${field} geçersiz IANA saat dilimi`);
  }
  return text;
}

function parsePlace(value: unknown, field: string): PlaceData | undefined {
  if (value === undefined || value === null) return undefined;
  if (!isRecord(value)) fail(`${field} nesne değil`);
  const place: PlaceData = {};
  const label = optionalString(value.label, `${field}.label`, MAX_PLACE_LENGTH);
  if (label !== undefined) place.label = label;
  const latitude = optionalNumber(value.latitude, `${field}.latitude`, -90, 90);
  if (latitude !== undefined) place.latitude = latitude;
  const longitude = optionalNumber(value.longitude, `${field}.longitude`, -180, 180);
  if (longitude !== undefined) place.longitude = longitude;
  return Object.keys(place).length > 0 ? place : undefined;
}

function parsePersonData(value: unknown, field: string): PersonData {
  if (!isRecord(value)) fail(`${field} nesne değil`);
  const person: PersonData = {
    birthDate: parseIsoDate(value.birthDate, `${field}.birthDate`)
  };
  const name = optionalString(value.name, `${field}.name`, MAX_NAME_LENGTH);
  if (name !== undefined) person.name = name;
  const birthTime = optionalString(value.birthTime, `${field}.birthTime`, 5);
  if (birthTime !== undefined) {
    if (!LOCAL_TIME.test(birthTime)) fail(`${field}.birthTime HH:mm biçiminde olmalı`);
    person.birthTime = birthTime;
  }
  const timeZone = parseTimeZone(value.timeZone, `${field}.timeZone`);
  if (timeZone !== undefined) person.timeZone = timeZone;
  const place = parsePlace(value.place, `${field}.place`);
  if (place !== undefined) person.place = place;
  return person;
}

function parseCardSelection(value: unknown, index: number): CardSelection {
  const field = `selectedCards[${index}]`;
  if (!isRecord(value)) fail(`${field} nesne değil`);
  const card: CardSelection = {
    position: optionalInteger(value.position, `${field}.position`, 0, 100) ?? index
  };
  const cardId = optionalString(value.cardId, `${field}.cardId`, 64);
  if (cardId !== undefined) card.cardId = cardId;
  const reversed = optionalBoolean(value.reversed, `${field}.reversed`);
  if (reversed !== undefined) card.reversed = reversed;
  return card;
}

function parseSymbolSelection(value: unknown, index: number): SymbolSelection {
  const field = `selectedSymbols[${index}]`;
  if (!isRecord(value)) fail(`${field} nesne değil`);
  return {
    symbolId: requiredString(value.symbolId, `${field}.symbolId`, 64),
    position: optionalInteger(value.position, `${field}.position`, 0, 100) ?? index
  };
}

function parseImageDescriptor(value: unknown, index: number): ImagePayloadDescriptor {
  const field = `imagePayload[${index}]`;
  if (!isRecord(value)) fail(`${field} nesne değil`);
  const role = requiredString(value.role, `${field}.role`, 16);
  if (!IMAGE_ROLES.has(role)) fail(`${field}.role geçersiz`);
  const mimeType = requiredString(value.mimeType, `${field}.mimeType`, 64);
  if (!IMAGE_MIME_TYPES.has(mimeType)) fail(`${field}.mimeType desteklenmiyor`);
  const reference = value.reference;
  if (!isRecord(reference)) fail(`${field}.reference nesne değil`);
  const kind = requiredString(reference.kind, `${field}.reference.kind`, 32);
  if (!REFERENCE_KINDS.has(kind)) fail(`${field}.reference.kind geçersiz`);

  const descriptor: ImagePayloadDescriptor = {
    assetId: requiredString(value.assetId, `${field}.assetId`, MAX_ID_LENGTH),
    role: role as ImagePayloadDescriptor["role"],
    mimeType,
    byteCount: optionalInteger(value.byteCount, `${field}.byteCount`, 0, 50_000_000) ?? 0,
    reference: {
      kind: kind as ImagePayloadDescriptor["reference"]["kind"],
      id: requiredString(reference.id, `${field}.reference.id`, MAX_ID_LENGTH)
    }
  };
  const width = optionalInteger(value.width, `${field}.width`, 1, 20_000);
  if (width !== undefined) descriptor.width = width;
  const height = optionalInteger(value.height, `${field}.height`, 1, 20_000);
  if (height !== undefined) descriptor.height = height;
  return descriptor;
}

function parseAdditionalInputs(value: unknown): AdditionalInputs | undefined {
  if (value === undefined || value === null) return undefined;
  if (!isRecord(value)) fail("additionalInputs nesne değil");
  const inputs: AdditionalInputs = {};
  const dreamText = optionalString(value.dreamText, "additionalInputs.dreamText", MAX_DREAM_TEXT_LENGTH);
  if (dreamText !== undefined) inputs.dreamText = dreamText;
  const deckId = optionalString(value.deckId, "additionalInputs.deckId", 64);
  if (deckId !== undefined) inputs.deckId = deckId;
  const deckVersion = optionalString(value.deckVersion, "additionalInputs.deckVersion", 32);
  if (deckVersion !== undefined) inputs.deckVersion = deckVersion;
  const spreadId = optionalString(value.spreadId, "additionalInputs.spreadId", 64);
  if (spreadId !== undefined) inputs.spreadId = spreadId;
  const castMethod = optionalString(value.castMethod, "additionalInputs.castMethod", 64);
  if (castMethod !== undefined) inputs.castMethod = castMethod;
  const targetDate = optionalString(value.targetDate, "additionalInputs.targetDate", 10);
  if (targetDate !== undefined) {
    if (!ISO_DATE.test(targetDate)) fail("additionalInputs.targetDate YYYY-MM-DD biçiminde olmalı");
    inputs.targetDate = targetDate;
  }
  return Object.keys(inputs).length > 0 ? inputs : undefined;
}

function parseArray<T>(
  value: unknown,
  field: string,
  parseItem: (item: unknown, index: number) => T
): T[] | undefined {
  if (value === undefined || value === null) return undefined;
  if (!Array.isArray(value)) fail(`${field} dizi değil`);
  if (value.length > MAX_LIST_ITEMS) fail(`${field} çok fazla öğe içeriyor`);
  return value.map((item, index) => parseItem(item, index));
}

/**
 * Validates the generic submission envelope and returns a normalized request.
 * Unknown fields are dropped and nested payloads are checked so engines never
 * receive an object that does not satisfy its declared type. Family engines
 * still perform their own method-specific validation.
 */
export function parseReadingRequest(raw: unknown): ReadingRequest {
  if (!isRecord(raw)) fail("istek bir nesne değil");
  if (raw.schemaVersion !== READING_SCHEMA_VERSION) {
    throw new ReadingContractError(
      "UNSUPPORTED_SCHEMA_VERSION",
      `desteklenmeyen schemaVersion: ${String(raw.schemaVersion)}`
    );
  }
  const requestId = requiredString(raw.requestId, "requestId", MAX_ID_LENGTH);
  const readingType = requiredString(raw.readingType, "readingType", 64);
  if (!(FORTUNE_TYPE_IDS as readonly string[]).includes(readingType)) {
    throw new ReadingContractError("UNKNOWN_READING_TYPE", `bilinmeyen fal türü: ${readingType}`);
  }

  const request: ReadingRequest = {
    schemaVersion: READING_SCHEMA_VERSION,
    requestId,
    readingType: readingType as FortuneTypeId
  };

  const question = optionalString(raw.question, "question", MAX_QUESTION_LENGTH);
  if (question !== undefined) request.question = question;

  const selectedCards = parseArray(raw.selectedCards, "selectedCards", parseCardSelection);
  if (selectedCards !== undefined) request.selectedCards = selectedCards;

  const selectedSymbols = parseArray(raw.selectedSymbols, "selectedSymbols", parseSymbolSelection);
  if (selectedSymbols !== undefined) request.selectedSymbols = selectedSymbols;

  const imagePayload = parseArray(raw.imagePayload, "imagePayload", parseImageDescriptor);
  if (imagePayload !== undefined) request.imagePayload = imagePayload;

  if (raw.birthData !== undefined && raw.birthData !== null) {
    request.birthData = parsePersonData(raw.birthData, "birthData");
  }
  if (raw.secondPersonData !== undefined && raw.secondPersonData !== null) {
    request.secondPersonData = parsePersonData(raw.secondPersonData, "secondPersonData");
  }

  const additionalInputs = parseAdditionalInputs(raw.additionalInputs);
  if (additionalInputs !== undefined) request.additionalInputs = additionalInputs;

  return request;
}
