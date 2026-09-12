import { HttpsError } from "firebase-functions/v2/https";

/** Sağlayıcı anahtarı yoksa fırlatılır; güvenli biçimde `internal`e eşlenir. */
export class MissingAiSecretError extends Error {
  constructor(message = "ZAI_API_KEY secret tanımlı değil.") {
    super(message);
    this.name = "MissingAiSecretError";
  }
}

const SAFE_SERVER_MESSAGE = "Buket şu anda yanıt veremedi, tekrar dener misin?";
const SAFE_RATE_MESSAGE = "Buket şu anda çok yoğun, birazdan tekrar dener misin?";
const SAFE_TIMEOUT_MESSAGE = "Yanıt zaman aşımına uğradı, tekrar dener misin?";

interface ErrorShape {
  status?: unknown;
  name?: unknown;
  code?: unknown;
  message?: unknown;
}

/**
 * Sağlayıcı (Z.AI) hatalarını güvenli uygulama hatalarına eşler.
 * Sağlayıcı anahtarı, ham stack trace veya tam sağlayıcı yanıtı asla dışarı
 * verilmez; yalnızca allowlist'lenmiş alanlar loglanır.
 *
 * ÖNEMLİ: sağlayıcı anahtar hatası (401/403/eksik secret) `internal` olarak
 * eşlenir — `unauthenticated` DEĞİL; çünkü Android bunu kullanıcı oturum
 * sorunu olarak yorumlar.
 */
export function mapProviderError(err: unknown, operation: string): HttpsError {
  if (err instanceof HttpsError) return err;

  const shape = (err ?? {}) as ErrorShape;
  const status = typeof shape.status === "number" ? shape.status : undefined;
  const name = typeof shape.name === "string" ? shape.name : "";
  const code = typeof shape.code === "string" ? shape.code : "";

  console.error("ai provider error", { operation, status, name, code });

  if (err instanceof MissingAiSecretError) {
    return new HttpsError("internal", SAFE_SERVER_MESSAGE, { code: "AI_SERVER_ERROR" });
  }
  if (status === 429) {
    return new HttpsError("resource-exhausted", SAFE_RATE_MESSAGE, { code: "RATE_LIMIT" });
  }
  if (status === 401 || status === 403) {
    return new HttpsError("internal", SAFE_SERVER_MESSAGE, { code: "AI_SERVER_ERROR" });
  }
  if (name.includes("Timeout") || code === "ETIMEDOUT" || code === "ESOCKETTIMEDOUT") {
    return new HttpsError("deadline-exceeded", SAFE_TIMEOUT_MESSAGE, { code: "AI_TIMEOUT" });
  }
  if (name === "APIConnectionError" || (status !== undefined && status >= 500)) {
    return new HttpsError("unavailable", SAFE_SERVER_MESSAGE, { code: "AI_SERVER_ERROR" });
  }
  return new HttpsError("internal", SAFE_SERVER_MESSAGE, { code: "AI_SERVER_ERROR" });
}

/** Loglama için yalnızca allowlist'lenmiş hata alanları. Sağlayıcı mesajı yok. */
export function safeErrorInfo(err: unknown): { name?: string; status?: number; code?: string } {
  const shape = (err ?? {}) as ErrorShape;
  return {
    name: typeof shape.name === "string" ? shape.name : undefined,
    status: typeof shape.status === "number" ? shape.status : undefined,
    code: typeof shape.code === "string" ? shape.code : undefined
  };
}
