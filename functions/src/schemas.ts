/**
 * OpenAI Responses API structured output şemaları + backend tarafı validator'lar.
 * JSON, validate edilmeden Firestore'a asla yazılmaz.
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

/** Görsel payload doğrulama: data URL / base64, magic bytes, boyut. */
export function validateImageData(
  base64: string,
  maxDecodedBytes: number
): { buffer: Buffer; mime: string } {
  let data = base64.trim();
  let mime = "image/jpeg";
  const dataUrlMatch = /^data:(image\/(?:jpeg|jpg|png|webp));base64,(.+)$/s.exec(data);
  if (dataUrlMatch) {
    mime = dataUrlMatch[1] === "image/jpg" ? "image/jpeg" : dataUrlMatch[1];
    data = dataUrlMatch[2];
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
  // Magic bytes: JPEG (FFD8FF) veya PNG (89504E47)
  const isJpeg = buffer[0] === 0xff && buffer[1] === 0xd8;
  const isPng = buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4e;
  if (!isJpeg && !isPng) {
    throw new Error("IMAGE_INVALID: desteklenmeyen format");
  }
  return { buffer, mime };
}
