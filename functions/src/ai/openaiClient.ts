import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";

/**
 * OPENAI_API_KEY asla source code / BuildConfig / git'e girmez.
 * Sadece Firebase Functions Secret olarak deploy edilir:
 *   firebase functions:secrets:set OPENAI_API_KEY
 */
export const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

export function getClient(): OpenAI {
  const apiKey = OPENAI_API_KEY.value();
  if (!apiKey) {
    throw new Error("OPENAI_API_KEY secret tanımlı değil.");
  }
  return new OpenAI({ apiKey });
}
