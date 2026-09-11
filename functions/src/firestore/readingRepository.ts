import { Timestamp } from "firebase-admin/firestore";
import { db } from "./db";
import { CoffeeResult } from "../schemas";
import { TimeWindow } from "../schemas";

export interface ChatMessageDoc {
  role: "user" | "assistant";
  content: string;
  createdAt: Timestamp;
}

export async function getRecentMessages(
  uid: string,
  conversationId: string,
  limit = 16
): Promise<ChatMessageDoc[]> {
  const snap = await db()
    .collection(`users/${uid}/conversations/${conversationId}/messages`)
    .orderBy("createdAt", "asc")
    .limitToLast(limit)
    .get();
  return snap.docs.map((d) => d.data() as ChatMessageDoc);
}

export async function appendMessage(
  uid: string,
  conversationId: string,
  role: "user" | "assistant",
  content: string
): Promise<void> {
  const now = Timestamp.now();
  const convRef = db().doc(`users/${uid}/conversations/${conversationId}`);
  await convRef.set({ updatedAt: now }, { merge: true });
  await convRef.collection("messages").add({
    role,
    content,
    createdAt: now
  });
}

export async function createConversation(uid: string): Promise<string> {
  const ref = await db()
    .collection(`users/${uid}/conversations`)
    .add({ createdAt: Timestamp.now(), updatedAt: Timestamp.now() });
  return ref.id;
}

export async function conversationExists(uid: string, conversationId: string): Promise<boolean> {
  const doc = await db().doc(`users/${uid}/conversations/${conversationId}`).get();
  return doc.exists;
}

export interface SavedReading {
  readingId: string;
}

export async function saveCoffeeReading(
  uid: string,
  result: CoffeeResult,
  model: string,
  question: string | undefined
): Promise<string> {
  const ref = await db()
    .collection(`users/${uid}/readings`)
    .add({
      type: "coffee",
      question: question ?? null,
      status: "completed",
      model,
      result,
      createdAt: Timestamp.now()
    });
  return ref.id;
}

/** Prediction Ledger V1: fal sonucundaki zaman pencereleri open status ile kaydedilir. */
export async function savePredictions(
  uid: string,
  readingId: string,
  windows: TimeWindow[]
): Promise<void> {
  if (windows.length === 0) return;
  const batch = db().batch();
  const col = db().collection(`users/${uid}/predictions`);
  const now = Timestamp.now();
  for (const w of windows.slice(0, 10)) {
    batch.set(col.doc(), {
      category: w.topic.toLowerCase().includes("aşk") || w.topic.toLowerCase().includes("sevgi")
        ? "relationship"
        : w.topic.toLowerCase().includes("iş") || w.topic.toLowerCase().includes("para")
          ? "career"
          : "other",
      statement: `${w.topic}: ${w.window}`,
      windowStart: null,
      windowEnd: null,
      status: "open",
      readingId,
      createdAt: now
    });
  }
  await batch.commit();
}
