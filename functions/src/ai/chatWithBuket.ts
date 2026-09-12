import { HttpsError, onCall } from "firebase-functions/v2/https";
import { OPENAI_API_KEY, getClient } from "../ai/openaiClient";
import { CHAT_MODEL, CHAT_DAILY_LIMIT } from "../config/openaiModels";
import { buketInstructions, MEMORY_EXTRACTION_INSTRUCTIONS } from "../prompts";
import { memoryExtractionJsonSchema, parseMemoryCandidates } from "../schemas";
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
  { secrets: [OPENAI_API_KEY], region: "europe-west1", timeoutSeconds: 120, memory: "256MiB" },
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

    const client = getClient();
    const input: OpenResponsesInput[] = history.map((m) => ({
      role: m.role === "user" ? "user" : "assistant",
      content: [{ type: m.role === "user" ? "input_text" : "output_text", text: m.content }]
    }));
    input.push({ role: "user", content: [{ type: "input_text", text: message }] });

    let reply: string;
    try {
      const response = await client.responses.create({
        model: CHAT_MODEL,
        store: false,
        instructions: buketInstructions(memoryBlock),
        input: input as never,
        max_output_tokens: 900
      });
      reply = (response as { output_text?: string }).output_text?.trim() || "";
      if (!reply) throw new Error("boş yanıt");
    } catch (err) {
      console.error("chatWithBuket OpenAI error", err);
      throw new HttpsError("unavailable", "Buket şu anda yanıt veremedi, tekrar dener misin?", { code: "AI_SERVER_ERROR" });
    }

    // Kalıcılık + kullanım sayacı.
    await appendMessage(uid, conversationId, "user", message);
    await appendMessage(uid, conversationId, "assistant", reply);
    await incrementUsage(uid, "chatCount");

    // Memory extraction: hata chat'i bozmasın.
    let memorySaved = false;
    try {
      const extraction = await client.responses.create({
        model: CHAT_MODEL,
        store: false,
        instructions: MEMORY_EXTRACTION_INSTRUCTIONS,
        input: [
          {
            role: "user",
            content: [
              {
                type: "input_text",
                text: `Kullanıcı mesajı: "${message}"\n\nFalcı yanıtı: "${reply.slice(0, 600)}"`
              }
            ]
          }
        ],
        text: { format: { type: "json_schema", name: "memory_extraction", schema: memoryExtractionJsonSchema, strict: true } },
        max_output_tokens: 500
      });
      const raw = JSON.parse((extraction as { output_text?: string }).output_text || "{}");
      const candidates = parseMemoryCandidates(raw);
      if (candidates.length > 0) {
        const merged = mergeMemories(memories, candidates);
        await applyMemoryMerge(uid, merged.toCreate, merged.toUpdate);
        memorySaved = merged.toCreate.length + merged.toUpdate.length > 0;
      }
      await touchMemories(uid, memories.slice(0, 6).map((m) => m.id!).filter(Boolean));
    } catch (err) {
      console.warn("memory extraction failed (non-fatal)", err);
    }

    // Response'a secret / stack trace sızmaz.
    return { conversationId, reply, memorySaved };
  }
);

type OpenResponsesInput = {
  role: "user" | "assistant";
  content: Array<{ type: string; text: string }>;
};
