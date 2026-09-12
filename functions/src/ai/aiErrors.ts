import OpenAI from "openai";
import { HttpsError } from "firebase-functions/v2/https";

/** Sağlayıcı anahtarı yoksa fırlatılır; güvenli biçimde `internal`e eşlenir. */
export class MissingAiSecretError extends Error {
  constructor(message = "ZAI_API_KEY secret tanımlı değil.") {
    super(message);
    this.name = "MissingAiSecretError";
  }
}

const SAFE_SERVER_MESSAGE = "Buket şu anda yanıt veremedi, tekrar dener misin?";
const SAFE_UNAVAILABLE_MESSAGE = "Buket şu anda geçici olarak yanıt veremiyor, birazdan tekrar dener misin?";
const SAFE_RATE_MESSAGE = "Buket şu anda çok yoğun, birazdan tekrar dener misin?";
const SAFE_TIMEOUT_MESSAGE = "Yanıt zaman aşımına uğradı, tekrar dener misin?";

export type ProviderErrorCategory =
  | "PROVIDER_TIMEOUT"
  | "PROVIDER_CONNECTION"
  | "PROVIDER_RATE_LIMIT"
  | "PROVIDER_AUTH"
  | "PROVIDER_BAD_REQUEST"
  | "PROVIDER_SERVER"
  | "PROVIDER_UNKNOWN";

export interface ProviderErrorClassification {
  category: ProviderErrorCategory;
  httpStatus?: number;
}

function httpStatusOf(err: unknown): number | undefined {
  const status = (err as { status?: unknown }).status;
  return typeof status === "number" ? status : undefined;
}

/**
 * Sağlayıcı hatalarını SDK kimliğine göre sınıflandırır. `name`/`code` alanlarına
 * GÜVENİLMEZ: openai@4.104.0 hata sınıfları `name` atamaz (Error'da kalır) ve
 * bağlantı hatalarında `code` boştur. Bu yüzden `instanceof` kullanılır.
 */
export function classifyProviderError(err: unknown): ProviderErrorClassification {
  if (err instanceof MissingAiSecretError) {
    return { category: "PROVIDER_AUTH" };
  }
  if (err instanceof OpenAI.APIConnectionTimeoutError) {
    return { category: "PROVIDER_TIMEOUT" };
  }
  if (err instanceof OpenAI.APIConnectionError) {
    return { category: "PROVIDER_CONNECTION" };
  }
  if (err instanceof OpenAI.RateLimitError) {
    return { category: "PROVIDER_RATE_LIMIT", httpStatus: httpStatusOf(err) ?? 429 };
  }
  if (err instanceof OpenAI.AuthenticationError || err instanceof OpenAI.PermissionDeniedError) {
    return { category: "PROVIDER_AUTH", httpStatus: httpStatusOf(err) };
  }
  if (err instanceof OpenAI.BadRequestError || err instanceof OpenAI.UnprocessableEntityError) {
    return { category: "PROVIDER_BAD_REQUEST", httpStatus: httpStatusOf(err) };
  }
  if (err instanceof OpenAI.InternalServerError) {
    return { category: "PROVIDER_SERVER", httpStatus: httpStatusOf(err) };
  }
  if (err instanceof OpenAI.APIError) {
    const status = httpStatusOf(err);
    if (status === 429) return { category: "PROVIDER_RATE_LIMIT", httpStatus: status };
    if (status === 401 || status === 403) return { category: "PROVIDER_AUTH", httpStatus: status };
    if (status !== undefined && status >= 500) return { category: "PROVIDER_SERVER", httpStatus: status };
    if (status !== undefined && status >= 400) return { category: "PROVIDER_BAD_REQUEST", httpStatus: status };
    return { category: "PROVIDER_UNKNOWN", httpStatus: status };
  }
  return { category: "PROVIDER_UNKNOWN" };
}

/**
 * Sağlayıcı hatalarını güvenli uygulama hatalarına eşler. Sağlayıcı anahtarı,
 * ham stack trace veya tam sağlayıcı yanıtı asla dışarı verilmez; yalnızca
 * allowlist'lenmiş kategori/HTTP durumu loglanır.
 *
 * ÖNEMLİ: sağlayıcı anahtar hatası (401/403/eksik secret) `internal` olarak
 * eşlenir — `unauthenticated` DEĞİL; çünkü Android bunu kullanıcı oturum
 * sorunu olarak yorumlar.
 */
export function mapProviderError(err: unknown, operation: string): HttpsError {
  if (err instanceof HttpsError) return err;

  const { category, httpStatus } = classifyProviderError(err);
  console.error("ai provider error", { operation, category, httpStatus });

  switch (category) {
    case "PROVIDER_TIMEOUT":
      return new HttpsError("deadline-exceeded", SAFE_TIMEOUT_MESSAGE, { code: "AI_TIMEOUT" });
    case "PROVIDER_CONNECTION":
    case "PROVIDER_SERVER":
      return new HttpsError("unavailable", SAFE_UNAVAILABLE_MESSAGE, { code: "AI_SERVER_ERROR" });
    case "PROVIDER_RATE_LIMIT":
      return new HttpsError("resource-exhausted", SAFE_RATE_MESSAGE, { code: "RATE_LIMIT" });
    case "PROVIDER_AUTH":
    case "PROVIDER_BAD_REQUEST":
    case "PROVIDER_UNKNOWN":
    default:
      return new HttpsError("internal", SAFE_SERVER_MESSAGE, { code: "AI_SERVER_ERROR" });
  }
}

/** Loglama için yalnızca allowlist'lenmiş kategori/HTTP durumu. Sağlayıcı mesajı yok. */
export function safeErrorInfo(err: unknown): { category: ProviderErrorCategory; httpStatus?: number } {
  return classifyProviderError(err);
}
