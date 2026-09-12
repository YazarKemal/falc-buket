/**
 * Sağlayıcıdan bağımsız structured output şemaları + backend validator'lar.
 * Model çıktısı JSON olarak ayrıştırılıp doğrulanmadan Firestore'a asla yazılmaz.
 */

export interface MemoryCandidate {
  category: string;
  subject: string;
  fact: string;
  importance: number;
  confidence: number;
}

export interface VisualObservation {
  observation: string;
  interpretation: string;
}

export interface TimeWindow {
  topic: string;
  window: string;
}

export interface CoffeeResult {
  title: string;
  summary: string;
  visualObservations: VisualObservation[];
  generalEnergy: string;
  love: string;
  careerMoney: string;
  nearFuture: string;
  highlight: string;
  timeWindows: TimeWindow[];
  memoryCandidates: MemoryCandidate[];
}

export const MEMORY_CATEGORIES = [
  "relationship", "career", "education", "family", "goal", "person", "event", "preference", "other"
] as const;

export const coffeeJsonSchema = {
  type: "object" as const,
  properties: {
    title: { type: "string", description: "Kısa, mistik ve özgün bir fal başlığı." },
    summary: { type: "string", description: "Falın 2-3 cümlelik özeti." },
    visualObservations: {
      type: "array",
      items: {
        type: "object",
        properties: {
          observation: { type: "string", description: "Fincanda GERÇEKTEN görülen şekil/iz, teknik betimleme." },
          interpretation: { type: "string", description: "Bu şeklin klasik fal sembolizminde karşılığı." }
        },
        required: ["observation", "interpretation"],
        additionalProperties: false
      },
      description: "Fincanda görülen şekiller ve yorumları. Görselde olmayan sembol uydurma."
    },
    generalEnergy: { type: "string" },
    love: { type: "string" },
    careerMoney: { type: "string" },
    nearFuture: { type: "string" },
    highlight: { type: "string", description: "Buket'in dikkatini çeken öne çıkan yorum." },
    timeWindows: {
      type: "array",
      items: {
        type: "object",
        properties: {
          topic: { type: "string" },
          window: { type: "string", description: "Örn: 'önümüzdeki 2-3 hafta'" }
        },
        required: ["topic", "window"],
        additionalProperties: false
      }
    },
    memoryCandidates: {
      type: "array",
      items: {
        type: "object",
        properties: {
          category: { type: "string", enum: [...MEMORY_CATEGORIES] },
          subject: { type: "string" },
          fact: { type: "string" },
          importance: { type: "number" },
          confidence: { type: "number" }
        },
        required: ["category", "subject", "fact", "importance", "confidence"],
        additionalProperties: false
      }
    }
  },
  required: ["title", "summary", "visualObservations", "generalEnergy", "love", "careerMoney", "nearFuture", "highlight", "timeWindows", "memoryCandidates"],
  additionalProperties: false
};

export const memoryExtractionJsonSchema = {
  type: "object" as const,
  properties: {
    memories: {
      type: "array",
      items: {
        type: "object",
        properties: {
          category: { type: "string", enum: [...MEMORY_CATEGORIES] },
          subject: { type: "string" },
          fact: { type: "string" },
          importance: { type: "number" },
          confidence: { type: "number" }
        },
        required: ["category", "subject", "fact", "importance", "confidence"],
        additionalProperties: false
      }
    }
  },
  required: ["memories"],
  additionalProperties: false
};

function isNonEmptyString(v: unknown, min = 1): boolean {
  return typeof v === "string" && v.trim().length >= min;
}

function isScore(v: unknown): boolean {
  return typeof v === "number" && Number.isFinite(v) && v >= 0 && v <= 1;
}

export function parseMemoryCandidates(raw: unknown): MemoryCandidate[] {
  if (!raw || typeof raw !== "object") return [];
  const list = (raw as { memories?: unknown }).memories;
  if (!Array.isArray(list)) return [];
  const out: MemoryCandidate[] = [];
  for (const item of list) {
    if (!item || typeof item !== "object") continue;
    const m = item as Record<string, unknown>;
    if (!isNonEmptyString(m.subject as string) || !isNonEmptyString(m.fact as string, 8)) continue;
    if (!(MEMORY_CATEGORIES as readonly string[]).includes(m.category as string)) continue;
    if (!isScore(m.importance) || !isScore(m.confidence)) continue;
    out.push({
      category: m.category as string,
      subject: (m.subject as string).trim().slice(0, 80),
      fact: (m.fact as string).trim().slice(0, 400),
      importance: m.importance as number,
      confidence: m.confidence as number
    });
  }
  return out.slice(0, 10);
}

export function validateCoffeeResult(raw: unknown): CoffeeResult {
  if (!raw || typeof raw !== "object") {
    throw new Error("INVALID_AI_RESPONSE: sonuç bir obje değil");
  }
  const r = raw as Record<string, unknown>;
  const req = (k: string): string => {
    if (!isNonEmptyString(r[k])) throw new Error(`INVALID_AI_RESPONSE: ${k} eksik veya boş`);
    return r[k] as string;
  };

  const observations: VisualObservation[] = Array.isArray(r.visualObservations)
    ? (r.visualObservations as unknown[])
        .filter((o): o is Record<string, unknown> => !!o && typeof o === "object")
        .filter((o) => isNonEmptyString(o.observation as string))
        .map((o) => ({
          observation: (o.observation as string).trim().slice(0, 300),
          interpretation: isNonEmptyString(o.interpretation as string) ? (o.interpretation as string).trim().slice(0, 500) : ""
        }))
    : [];

  const windows: TimeWindow[] = Array.isArray(r.timeWindows)
    ? (r.timeWindows as unknown[])
        .filter((w): w is Record<string, unknown> => !!w && typeof w === "object")
        .filter((w) => isNonEmptyString(w.topic as string) && isNonEmptyString(w.window as string))
        .map((w) => ({
          topic: (w.topic as string).trim().slice(0, 120),
          window: (w.window as string).trim().slice(0, 160)
        }))
    : [];

  return {
    title: req("title").slice(0, 120),
    summary: req("summary").slice(0, 2000),
    visualObservations: observations,
    generalEnergy: req("generalEnergy").slice(0, 3000),
    love: req("love").slice(0, 3000),
    careerMoney: req("careerMoney").slice(0, 3000),
    nearFuture: req("nearFuture").slice(0, 3000),
    highlight: req("highlight").slice(0, 1200),
    timeWindows: windows,
    memoryCandidates: parseMemoryCandidates({ memories: r.memoryCandidates })
  };
}

/**
 * Backend tarafından kabul edilen görsel formatları. Android ön işleme her zaman
 * JPEG üretir; burada sadece JPEG ve PNG kabul edilir (WEBP desteklenmiyor).
 */
const SUPPORTED_IMAGE_MIME = new Set(["image/jpeg", "image/jpg", "image/png"]);

/** MIME'ı bytes'tan türet: JPEG (FFD8) veya PNG (89504E47). */
function detectImageMime(buffer: Buffer): "image/jpeg" | "image/png" | undefined {
  const isJpeg = buffer[0] === 0xff && buffer[1] === 0xd8;
  const isPng = buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4e && buffer[3] === 0x47;
  if (isJpeg) return "image/jpeg";
  if (isPng) return "image/png";
  return undefined;
}

/**
 * Görsel payload doğrulama: data URL / raw base64, magic bytes, boyut.
 * Dönen MIME, gövde bytes'ından türetilir; `data:` başlığı yalnızca bir
 * etikettir ve kaynak doğruluk kabul edilmez.
 */
export function validateImageData(
  base64: string,
  maxDecodedBytes: number
): { buffer: Buffer; mime: string } {
  let data = base64.trim();
  if (/^data:/i.test(data)) {
    const match = /^data:([^;,]+);base64,(.+)$/is.exec(data);
    if (!match) {
      throw new Error("IMAGE_INVALID: geçersiz data URL");
    }
    if (!SUPPORTED_IMAGE_MIME.has(match[1].toLowerCase())) {
      throw new Error("IMAGE_INVALID: desteklenmeyen format");
    }
    data = match[2];
  }
  if (data.length < 8) {
    throw new Error("IMAGE_INVALID: payload çok küçük");
  }
  if (data.length > Math.ceil(maxDecodedBytes / 3) * 4 + 4) {
    throw new Error("IMAGE_TOO_LARGE");
  }
  const buffer = Buffer.from(data, "base64");
  if (buffer.length === 0 || !/^[A-Za-z0-9+/=\r\n]+$/.test(data.slice(0, 256))) {
    throw new Error("IMAGE_INVALID: base64 geçersiz");
  }
  if (buffer.length > maxDecodedBytes) {
    throw new Error("IMAGE_TOO_LARGE");
  }
  const mime = detectImageMime(buffer);
  if (!mime) {
    throw new Error("IMAGE_INVALID: desteklenmeyen format");
  }
  return { buffer, mime };
}

/**
 * Doğrulanmış bir görselden, kendi tespit edilen MIME'ını taşıyan bir data URL
 * üretir. Cup ve tabak bağımsız çağrılır; biri diğerinin MIME'ını varsaymaz.
 */
export function toValidatedImageDataUrl(
  base64: string,
  maxDecodedBytes: number
): { mime: string; dataUrl: string } {
  const { mime } = validateImageData(base64, maxDecodedBytes);
  const trimmed = base64.trim();
  const data = trimmed.includes(",") ? trimmed.slice(trimmed.indexOf(",") + 1) : trimmed;
  return { mime, dataUrl: `data:${mime};base64,${data}` };
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** Markdown ```json ... ``` çitlerini kaldırır. */
function stripCodeFences(text: string): string {
  const fenced = /^```(?:json)?\s*([\s\S]*?)\s*```$/i.exec(text);
  return fenced ? fenced[1].trim() : text;
}

/**
 * Metin içindeki tek ve belirsiz olmayan dengeli JSON nesnesini çıkarır.
 * String kaçışlarına saygı gösterir; ikinci bir nesne varsa reddeder.
 */
function extractSingleObject(text: string): string | undefined {
  const start = text.indexOf("{");
  if (start === -1) return undefined;
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let i = start; i < text.length; i++) {
    const ch = text[i];
    if (inString) {
      if (escaped) escaped = false;
      else if (ch === "\\") escaped = true;
      else if (ch === '"') inString = false;
      continue;
    }
    if (ch === '"') inString = true;
    else if (ch === "{") depth++;
    else if (ch === "}") {
      depth--;
      if (depth === 0) {
        if (text.slice(i + 1).includes("{")) return undefined; // birden fazla nesne
        return text.slice(start, i + 1);
      }
    }
  }
  return undefined;
}

/**
 * Model çıktısından JSON nesnesi ayrıştırır. Deterministik ve fail-closed:
 * dizi/primitive kökleri, çoklu nesneleri ve bozuk/kesik JSON'u reddeder.
 * Hata mesajına model çıktısı gömülmez.
 */
export function parseJsonObjectFromText(text: string): Record<string, unknown> {
  const cleaned = stripCodeFences(text.replace(/^\uFEFF/, "").trim());
  const candidate = cleaned.startsWith("{") ? cleaned : extractSingleObject(cleaned);
  if (!candidate) {
    throw new Error("INVALID_AI_RESPONSE: geçerli JSON nesnesi bulunamadı");
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(candidate);
  } catch {
    throw new Error("INVALID_AI_RESPONSE: JSON ayrıştırılamadı");
  }
  if (!isPlainObject(parsed)) {
    throw new Error("INVALID_AI_RESPONSE: kök bir nesne değil");
  }
  return parsed;
}

function assertArrayField(
  value: unknown,
  field: string,
  itemCheck: (item: unknown) => boolean
): void {
  if (!Array.isArray(value)) {
    throw new Error(`INVALID_AI_RESPONSE: ${field} dizi değil`);
  }
  for (const item of value) {
    if (!itemCheck(item)) {
      throw new Error(`INVALID_AI_RESPONSE: ${field} öğesi geçersiz`);
    }
  }
}

/**
 * Coffee sonucunun yapısal zarfını doğrular (normalize etmeden önce).
 * Eksik/yanlış diziler sessizce boş kabul edilmesin diye eklenmiştir.
 * Boş diziler geçerlidir.
 */
export function assertCoffeeEnvelope(raw: unknown): void {
  if (!isPlainObject(raw)) {
    throw new Error("INVALID_AI_RESPONSE: sonuç bir nesne değil");
  }
  for (const key of ["title", "summary", "generalEnergy", "love", "careerMoney", "nearFuture", "highlight"]) {
    if (!isNonEmptyString(raw[key])) {
      throw new Error(`INVALID_AI_RESPONSE: ${key} eksik veya boş`);
    }
  }
  assertArrayField(
    raw.visualObservations,
    "visualObservations",
    (item) => isPlainObject(item) && isNonEmptyString(item.observation as string)
  );
  assertArrayField(
    raw.timeWindows,
    "timeWindows",
    (item) => isPlainObject(item) && isNonEmptyString(item.topic as string) && isNonEmptyString(item.window as string)
  );
  assertArrayField(raw.memoryCandidates, "memoryCandidates", (item) => isPlainObject(item));
}

/** Memory extraction zarfı: `memories` bir dizi olmalı. Boş dizi geçerlidir. */
export function assertMemoryEnvelope(raw: unknown): void {
  if (!isPlainObject(raw) || !Array.isArray(raw.memories)) {
    throw new Error("INVALID_AI_RESPONSE: memories dizisi yok");
  }
}
