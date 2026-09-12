/**
 * Küçük sağlayıcı soyutlaması. Amaç, Memory Engine / Coffee akışı / Android
 * UI'yi değiştirmeden ileride model veya sağlayıcı değiştirebilmektir.
 * Bilinçli olarak minimal tutulur; kayıt defteri, fallback veya SDK tipleri
 * dışarı sızmaz.
 */

export interface AiTextTurn {
  role: "user" | "assistant";
  content: string;
}

export interface GenerateTextInput {
  system: string;
  turns: AiTextTurn[];
  maxTokens: number;
  /** Metin modelinde JSON nesne modu ister. Vision için kullanılmaz. */
  json?: boolean;
  timeoutMs?: number;
}

export interface AnalyzeImagesInput {
  system: string;
  prompt: string;
  /** Doğrulanmış data URL'ler (data:<mime>;base64,...). */
  images: string[];
  maxTokens: number;
  timeoutMs?: number;
}

export interface AiProvider {
  /** Geçerli, boş olmayan metin döndürür; aksi halde hata fırlatır. */
  generateText(input: GenerateTextInput): Promise<string>;
  /** Görselleri yorumlayıp geçerli, boş olmayan metin döndürür. */
  analyzeImages(input: AnalyzeImagesInput): Promise<string>;
}
