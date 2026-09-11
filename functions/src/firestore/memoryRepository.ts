import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { db } from "./db";
import { MemoryRecord } from "../memory";
import { MemoryCandidate } from "../schemas";

export async function topMemories(uid: string, limit = 12): Promise<MemoryRecord[]> {
  const snap = await db()
    .collection(`users/${uid}/memories`)
    .orderBy("importance", "desc")
    .limit(limit)
    .get();
  return snap.docs.map((d) => ({ id: d.id, ...d.data() } as MemoryRecord));
}

export async function applyMemoryMerge(
  uid: string,
  toCreate: MemoryCandidate[],
  toUpdate: { id: string; fact: string; importance: number; confidence: number }[]
): Promise<void> {
  const batch = db().batch();
  const col = db().collection(`users/${uid}/memories`);
  const now = Timestamp.now();

  for (const cand of toCreate) {
    batch.set(col.doc(), {
      category: cand.category,
      subject: cand.subject,
      fact: cand.fact,
      importance: cand.importance,
      confidence: cand.confidence,
      active: true,
      createdAt: now,
      updatedAt: now,
      lastUsedAt: now
    });
  }
  for (const upd of toUpdate) {
    batch.update(col.doc(upd.id), {
      fact: upd.fact,
      importance: upd.importance,
      confidence: upd.confidence,
      updatedAt: now,
      lastUsedAt: now
    });
  }
  await batch.commit();
}

export async function touchMemories(uid: string, ids: string[]): Promise<void> {
  if (ids.length === 0) return;
  const batch = db().batch();
  const now = Timestamp.now();
  for (const id of ids.slice(0, 12)) {
    batch.update(db().doc(`users/${uid}/memories/${id}`), { lastUsedAt: now });
  }
  await batch.commit();
}

export async function clearMemories(uid: string): Promise<void> {
  const snap = await db().collection(`users/${uid}/memories`).get();
  const batch = db().batch();
  snap.docs.forEach((d) => batch.delete(d.ref));
  await batch.commit();
}

export async function incrementUsage(uid: string, field: "chatCount" | "visionCount"): Promise<void> {
  const period = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const ref = db().doc(`users/${uid}/usage/${period}`);
  await ref.set({ [field]: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
}

export async function getTodayUsage(
  uid: string
): Promise<{ chatCount: number; visionCount: number }> {
  const period = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const doc = await db().doc(`users/${uid}/usage/${period}`).get();
  const data = doc.data() ?? {};
  return { chatCount: Number(data.chatCount ?? 0), visionCount: Number(data.visionCount ?? 0) };
}
