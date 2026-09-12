/**
 * Tek merkezi AI sağlayıcı/model konfigürasyonu.
 * Model veya sağlayıcı değiştirmek için yalnızca bu dosya güncellenir.
 */
export const AI_BASE_URL = "https://api.z.ai/api/paas/v4/";

export const TEXT_MODEL = "glm-5.1";
export const VISION_MODEL = "glm-5v-turbo";

/** Maliyet kontrolü: kullanıcı bazında günlük soft limitler. */
export const CHAT_DAILY_LIMIT = 100;
export const VISION_DAILY_LIMIT = 20;

/** Yanıt token bütçeleri. */
export const CHAT_MAX_TOKENS = 900;
export const MEMORY_MAX_TOKENS = 500;
export const VISION_MAX_TOKENS = 2200;

/**
 * İstek zaman aşımı bütçeleri. Chat iki sıralı çağrı yapar (45 + 20 sn) ve
 * vision tek çağrıdır. Vision için güvenli hiyerarşi:
 *   provider (150 sn) < function (180 sn) < Android callable (210 sn)
 */
export const TEXT_REQUEST_TIMEOUT_MS = 45_000;
export const MEMORY_REQUEST_TIMEOUT_MS = 20_000;
export const VISION_REQUEST_TIMEOUT_MS = 150_000;

/** Vision fonksiyonunun Cloud Functions deadline'ı (sn). */
export const VISION_FUNCTION_TIMEOUT_SECONDS = 180;

/** Android Coffee callable zaman aşımı (sn). Provider'dan ~30 sn sonra. */
export const VISION_CLIENT_TIMEOUT_SECONDS = 210;

/**
 * Chat hiyerarşisi: iki sıralı metin çağrısı (45 + 20 sn) < fonksiyon < istemci.
 * Coffee'nin vision bütçeleriyle karıştırılmamalıdır.
 */
export const CHAT_FUNCTION_TIMEOUT_SECONDS = 120;
export const CHAT_CLIENT_TIMEOUT_SECONDS = 150;

/** Ağ hatalarında sessiz yeniden deneme yok; hata güvenli biçimde eşlenir. */
export const AI_MAX_RETRIES = 0;
