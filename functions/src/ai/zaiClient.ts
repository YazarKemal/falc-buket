import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";
import { AI_BASE_URL, AI_MAX_RETRIES } from "../config/aiModels";
import { MissingAiSecretError } from "./aiErrors";

/**
 * Z.AI API anahtarı asla source code / BuildConfig / git'e girmez.
 * Yalnızca Firebase Functions Secret olarak deploy edilir:
 *   firebase functions:secrets:set ZAI_API_KEY
 */
export const ZAI_API_KEY = defineSecret("ZAI_API_KEY");

/**
 * Z.AI, OpenAI SDK uyumlu olduğu için aynı `openai` paketi kullanılır; yalnızca
 * baseURL ve apiKey Z.AI'ye yönlendirilir. Sağlayıcı anahtarı çağrı anında
 * okunur (modül yüklenirken değil), böylece deploy/analiz aşamasında erişilmez.
 */
export function createZaiClient(): OpenAI {
  const apiKey = ZAI_API_KEY.value();
  if (!apiKey) {
    throw new MissingAiSecretError();
  }
  // maxRetries 0: bir kullanıcı gönderimi en fazla bir sağlayıcı isteği üretir.
  return new OpenAI({ apiKey, baseURL: AI_BASE_URL, maxRetries: AI_MAX_RETRIES });
}
