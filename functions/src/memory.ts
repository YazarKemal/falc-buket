import { MemoryCandidate } from "./schemas";

export interface MemoryRecord {
  id?: string;
  category: string;
  subject: string;
  fact: string;
  importance: number;
  confidence: number;
  active?: boolean;
  createdAt?: unknown;
  updatedAt?: unknown;
}

function norm(s: string): string {
  return s
    .toLocaleLowerCase("tr-TR")
    .replace(/[^\p{L}\p{N}\s]/gu, "")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Duplicate memory üretmemek için merge mantığı (pure, test edilebilir).
 * Aynı (category + normalized subject) çifti mevcutsa:
 *  - yeni confidence + importance daha yüksekse fact'i güncelle,
 *  - değilse mevcut kaydı koru (fact değişikliği yok).
 * Dönen yapı: { toCreate, toUpdate } — Firestore yazma işlemleri repository'de.
 */
export function mergeMemories(
  existing: MemoryRecord[],
  incoming: MemoryCandidate[]
): {
  toCreate: MemoryCandidate[];
  toUpdate: { id: string; fact: string; importance: number; confidence: number }[];
} {
  const byKey = new Map<string, MemoryRecord>();
  for (const e of existing) {
    byKey.set(`${e.category}::${norm(e.subject)}`, e);
  }

  const toCreate: MemoryCandidate[] = [];
  const toUpdate: { id: string; fact: string; importance: number; confidence: number }[] = [];

  for (const cand of incoming) {
    const key = `${cand.category}::${norm(cand.subject)}`;
    const found = byKey.get(key);
    if (!found) {
      toCreate.push(cand);
      byKey.set(key, { category: cand.category, subject: cand.subject, fact: cand.fact, importance: cand.importance, confidence: cand.confidence });
    } else if (found.id) {
      const stronger =
        cand.importance >= (found.importance ?? 0) - 0.05 &&
        cand.confidence >= (found.confidence ?? 0) * 0.9;
      if (stronger && norm(cand.fact) !== norm(found.fact)) {
        toUpdate.push({
          id: found.id,
          fact: cand.fact,
          importance: Math.max(cand.importance, found.importance ?? 0),
          confidence: Math.max(cand.confidence, found.confidence ?? 0)
        });
      }
    }
  }
  return { toCreate, toUpdate };
}

/** Bağlam bloğu: token bütçesini kontrol altında tutmak için fact'leri birleştirir. */
export function buildMemoryBlock(memories: MemoryRecord[], maxFacts = 12): string {
  const active = memories.filter((m) => m.active !== false).slice(0, maxFacts);
  if (active.length === 0) return "Bu kullanıcı hakkında kayıtlı hafıza bilgisi henüz yok.";
  return "Kullanıcı hakkında bilinenler:\n" + active.map((m) => `- (${m.category}) ${m.fact}`).join("\n");
}
