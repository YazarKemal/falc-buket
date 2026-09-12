import { HttpsError, onCall } from "firebase-functions/v2/https";
import { ZAI_API_KEY } from "./zaiClient";
import { getAiProvider } from "./zaiProvider";
import { mapProviderError, safeErrorInfo } from "./aiErrors";
import { AiTextTurn } from "./aiProvider";
import {
  CHAT_DAILY_LIMIT,
  CHAT_MAX_TOKENS,
  MEMORY_MAX_TOKENS,
  MEMORY_REQUEST_TIMEOUT_MS
} from "../config/aiModels";
import { buketInstructions, MEMORY_EXTRACTION_INSTRUCTIONS } from "../prompts";
import {
  memoryExtractionJsonSchema,
  parseMemoryCandidates,
  parseJsonObjectFromText,
  assertMemoryEnvelope
} from "../schemas";
import { mergeMemories, buildMemoryBlock } from "../memory";
import { topMemories, applyMemoryMerge, touchMemories, incrementUsage, getTodayUsage } from "../firestore/memoryRepository";
import {
  getRecentMessages,
  appendMessage,
  createConversation,
  conversationExists
} from "../firestore/readingRepository";

// TODO(production): Firebase App Check zorunlu kılınacak (enforceAppCheck: true).
export const chatWithBuket = onCall(
  { secrets: [ZAI_API_KEY], region: "europe-west1", timeoutSeconds: 120, memory: "256MiB" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Oturum gerekli.");
    }
    const uid = request.auth.uid;

    const message = (request.data?.message as string | undefined)?.trim();
    const conversationIdInput = request.data?.conversationId as string | undefined;
    if (!message || message.length < 1) {
      throw new HttpsError("invalid-argument", "Mesaj boş olamaz.");
    }
    if (message.length > 4000) {
      throw new HttpsError("invalid-argument", "Mesaj çok uzun.");
    }

    // Soft kota (maliyet kontrolü; premium limitleri buraya bağlanacak).
    const usage = await getTodayUsage(uid);
    if (usage.chatCount >= CHAT_DAILY_LIMIT) {
      throw new HttpsError("resource-exhausted", "Günlük sohbet limitine ulaşıldı.", { code: "RATE_LIMIT" });
    }

    // Konuşma sahipliği doğrula, yoksa oluştur.
    let conversationId = conversationIdInput;
    if (conversationId) {
      const exists = await conversationExists(uid, conversationId);
      if (!exists) throw new HttpsError("invalid-argument", "Geçersiz konuşma.", { code: "INVALID_CONVERSATION" });
    } else {
      conversationId = await createConversation(uid);
    }

    // Bağlam: memory + son mesajlar (token bütçesi kontrollü).
    const memories = await topMemories(uid, 12);
    const memoryBlock = buildMemoryBlock(memories);
    const history = await getRecentMessages(uid, conversationId, 14);

    const turns: AiTextTurn[] = history.map((m) => ({
      role: m.role === "user" ? "user" : "assistant",
      content: m.content
    }));
    turns.push({ role: "user", content: message });

    let reply: string;
    try {
      reply = await getAiProvider().generateText({
        system: buketInstructions(memoryBlock),
        turns,
        maxTokens: CHAT_MAX_TOKENS
      });
    } catch (err) {
      throw mapProviderError(err, "chatWithBuket");
    }

    // Kalıcılık + kullanım sayacı.
    await appendMessage(uid, conversationId, "user", message);
    await appendMessage(uid, conversationId, "assistant", reply);
    await incrementUsage(uid, "chatCount");

    // Memory extraction: hata chat'i bozmasın.
    let memorySaved = false;
    try {
      const memorySystem = `${MEMORY_EXTRACTION_INSTRUCTIONS}\n\nJSON şeması:\n${JSON.stringify(memoryExtractionJsonSchema)}`;
      const extractionText = await getAiProvider().generateText({
        system: memorySystem,
        turns: [
          {
            role: "user",
            content: `Kullanıcı mesajı: "${message}"\n\nFalcı yanıtı: "${reply.slice(0, 600)}"`
          }
        ],
        maxTokens: MEMORY_MAX_TOKENS,
        json: true,
        timeoutMs: MEMORY_REQUEST_TIMEOUT_MS
      });
      const raw = parseJsonObjectFromText(extractionText);
      assertMemoryEnvelope(raw);
      const candidates = parseMemoryCandidates(raw);
      if (candidates.length > 0) {
        const merged = mergeMemories(memories, candidates);
        await applyMemoryMerge(uid, merged.toCreate, merged.toUpdate);
        memorySaved = merged.toCreate.length + merged.toUpdate.length > 0;
      }
      await touchMemories(uid, memories.slice(0, 6).map((m) => m.id!).filter(Boolean));
    } catch (err) {
      console.warn("memory extraction failed (non-fatal)", safeErrorInfo(err));
    }

    // Response'a secret / stack trace sızmaz.
    return { conversationId, reply, memorySaved };
  }
);
