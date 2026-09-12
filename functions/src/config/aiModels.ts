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
 * İstek zaman aşımı bütçeleri. Fonksiyon deadline'ı 120 sn; chat iki sıralı
 * çağrı yapar (45 + 20 sn) ve vision tek çağrıdır (90 sn).
 */
export const TEXT_REQUEST_TIMEOUT_MS = 45_000;
export const MEMORY_REQUEST_TIMEOUT_MS = 20_000;
export const VISION_REQUEST_TIMEOUT_MS = 90_000;

/** Ağ hatalarında sessiz yeniden deneme yok; hata güvenli biçimde eşlenir. */
export const AI_MAX_RETRIES = 0;
