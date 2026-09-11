import { HttpsError, onCall } from "firebase-functions/v2/https";
import { OPENAI_API_KEY, getClient } from "../ai/openaiClient";
import { VISION_MODEL, VISION_DAILY_LIMIT } from "../config/openaiModels";
import { buketInstructions } from "../prompts";
import { coffeeJsonSchema, validateCoffeeResult, validateImageData, CoffeeResult } from "../schemas";
import { buildMemoryBlock } from "../memory";
import { topMemories, applyMemoryMerge, incrementUsage, getTodayUsage } from "../firestore/memoryRepository";
import { saveCoffeeReading, savePredictions } from "../firestore/readingRepository";

const MAX_DECODED_IMAGE_BYTES = 6_000_000; // ~6MB decoded; client ~2.5MB JPEG gönderir
const COFFEE_PROMPT = [
  "Kullanıcı bir Türk kahvesi fincanı fotoğrafı paylaşacak. Sen bu fotoğrafa gerçekten bakıp klasik Türk kahve falı geleneğine göre yorum yapacaksın.",
  "",
  "ANALİZ ADIMLARI:",
  "1. Fincanın genel telve yoğunluğu ve dağılımı.",
  "2. Açık / kapalı alanlar (fal geleneğinde açık alanlar iyiye işaret sayılır).",
  "3. Dip bölgesi (geçmiş / kapanan konular).",
  "4. Kenarlara yakın şekiller (yakın gelecek / çevre).",
  "5. Çizgiler / yollar.",
  "6. İnsan veya hayvanı andıran şekiller.",
  "7. Harf veya geometrik şekle benzeyen izler.",
  "8. Tabak fotoğrafı varsa tabaktaki telve dağılımı ve fincanla ilişkisi.",
  "",
  "KRİTİK KURALLAR:",
  "- SADECE görselde gerçekten görebildiğin izleri rapor et. Görselde olmayan sembol/şekil UYDURMA.",
  "- Görsel bulanık/karanlık ise görebildiğin kadarını söyle ve dürüst ol.",
  "- Her şekil için önce OBSERVATION (gerçek görsel iz), sonra INTERPRETATION (fal yorumu) yaz.",
  "- Yorumlar Buket'in sıcak, sezgisel sesiyle olsun; mekanik teknik analiz dili kullanıcıya gösterilmez ama observation alanı net betimleme içerir.",
  "- Kullanıcının hafıza bağlamı verilmişse yorumu o bağlamla doğal biçimde ilişkilendir; ancak bağlamdaki bilgiyi fotoğrafta 'görmüş' gibi sunma.",
  "- Kesin tarih/kesin sonuç verme; zaman pencereleri ve ihtimal dili kullan.",
  "- memoryCandidates alanına kullanıcıyla ilgili kalıcı bilgiler varsa ekle (görselden değil, bağlamdan).",
  ""
].join("\n");

// TODO(production): Firebase App Check zorunlu kılınacak (enforceAppCheck: true).
export const analyzeCoffeeReading = onCall(
  { secrets: [OPENAI_API_KEY], region: "europe-west1", timeoutSeconds: 120, memory: "512MiB" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Oturum gerekli.");
    }
    const uid = request.auth.uid;

    const cupBase64 = request.data?.cupImageBase64 as string | undefined;
    const saucerBase64 = request.data?.saucerImageBase64 as string | undefined;
    const userQuestion = (request.data?.userQuestion as string | undefined)?.trim();

    if (!cupBase64 || typeof cupBase64 !== "string") {
      throw new HttpsError("invalid-argument", "Fincan fotoğrafı gerekli.", { code: "IMAGE_INVALID" });
    }
    if (userQuestion && userQuestion.length > 500) {
      throw new HttpsError("invalid-argument", "Soru çok uzun.");
    }

    const usage = await getTodayUsage(uid);
    if (usage.visionCount >= VISION_DAILY_LIMIT) {
      throw new HttpsError("resource-exhausted", "Günlük fal limitine ulaşıldı.", { code: "RATE_LIMIT" });
    }

    // Görsel doğrulama (secret/stack sızmadan, ayrıntılı kod ile).
    let cupMime = "image/jpeg";
    let saucerDataUrl: string | undefined;
    try {
      cupMime = validateImageData(cupBase64, MAX_DECODED_IMAGE_BYTES).mime;
      if (saucerBase64) {
        validateImageData(saucerBase64, MAX_DECODED_IMAGE_BYTES);
        saucerDataUrl = toDataUrl(saucerBase64);
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      if (msg.startsWith("IMAGE_TOO_LARGE")) {
        throw new HttpsError("invalid-argument", "Fotoğraf çok büyük.", { code: "IMAGE_TOO_LARGE" });
      }
      throw new HttpsError("invalid-argument", "Fotoğraf geçersiz.", { code: "IMAGE_INVALID" });
    }

    const memories = await topMemories(uid, 8);
    const memoryBlock = buildMemoryBlock(memories);

    const cupDataUrl = toDataUrl(cupBase64, cupMime);
    const content: Array<{ type: string; text?: string; image_url?: string }> = [
      {
        type: "input_text",
        text: [
          userQuestion
            ? `Kullanıcının sorusu: "${userQuestion}"`
            : "Kullanıcı spesifik bir soru sormadı; genel bir kahve falı yorumu yap.",
          "",
          COFFEE_PROMPT
        ].join("\n")
      },
      { type: "input_image", image_url: cupDataUrl }
    ];
    if (saucerDataUrl) {
      content.push({ type: "input_image", image_url: saucerDataUrl });
    }

    const client = getClient();
    let result: CoffeeResult;
    try {
      const response = await client.responses.create({
        model: VISION_MODEL,
        instructions: buketInstructions(memoryBlock),
        input: [{ role: "user", content: content as never }],
        text: { format: { type: "json_schema", name: "coffee_reading", schema: coffeeJsonSchema, strict: true } },
        max_output_tokens: 2200
      });
      const raw = JSON.parse((response as { output_text?: string }).output_text || "{}");
      result = validateCoffeeResult(raw);
    } catch (err) {
      if (err instanceof Error && err.message.startsWith("INVALID_AI_RESPONSE")) {
        console.error("coffee result validation failed", err);
        throw new HttpsError("internal", "Buket yorumu tamamlayamadı, tekrar deneyebilirsin.", { code: "AI_SERVER_ERROR" });
      }
      console.error("analyzeCoffeeReading OpenAI error", err);
      throw new HttpsError("unavailable", "Buket şu anda fincanını yorumlayamadı, tekrar dener misin?", { code: "AI_SERVER_ERROR" });
    }

    // Kalıcılık: reading + prediction ledger + memory + usage.
    const readingId = await saveCoffeeReading(uid, result, VISION_MODEL, userQuestion);
    try {
      await savePredictions(uid, readingId, result.timeWindows);
      if (result.memoryCandidates.length > 0) {
        const { mergeMemories } = await import("../memory");
        const merged = mergeMemories(memories, result.memoryCandidates);
        await applyMemoryMerge(uid, merged.toCreate, merged.toUpdate);
      }
    } catch (err) {
      console.warn("post-reading persistence partially failed (non-fatal)", err);
    }
    await incrementUsage(uid, "visionCount");

    // Görsel payload'ları response'a ASLA ekleme.
    return { readingId, result };
  }
);

function toDataUrl(base64: string, mime = "image/jpeg"): string {
  const data = base64.includes(",") ? base64.slice(base64.indexOf(",") + 1) : base64;
  return `data:${mime};base64,${data}`;
}
