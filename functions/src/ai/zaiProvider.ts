import OpenAI from "openai";
import { createZaiClient } from "./zaiClient";
import { classifyProviderError } from "./aiErrors";
import {
  AI_MAX_RETRIES,
  TEXT_MODEL,
  TEXT_REQUEST_TIMEOUT_MS,
  VISION_MODEL,
  VISION_REQUEST_TIMEOUT_MS
} from "../config/aiModels";
import { AiProvider, AnalyzeImagesInput, GenerateTextInput } from "./aiProvider";

type ChatMessage = OpenAI.Chat.Completions.ChatCompletionMessageParam;
type ContentPart = OpenAI.Chat.Completions.ChatCompletionContentPart;
type ChatCompletion = OpenAI.Chat.Completions.ChatCompletion;

/**
 * Z.AI multimodal içerik dizisi: önce görsel blokları, sonra tek metin bloğu.
 * Saf fonksiyon; cup ve tabak bağımsız data URL'ler olarak korunur.
 */
export function buildVisionContent(images: string[], prompt: string): ContentPart[] {
  return [
    ...images.map((url) => ({ type: "image_url" as const, image_url: { url } })),
    { type: "text" as const, text: prompt }
  ];
}

/** Boş, null veya kesilmiş (length) tamamlamaları reddeder. */
export function extractCompletionText(response: ChatCompletion): string {
  const choice = response.choices?.[0];
  const content = choice?.message?.content;
  if (typeof content !== "string" || content.trim().length === 0) {
    throw new Error("AI_EMPTY_RESPONSE: boş yanıt");
  }
  if (choice?.finish_reason === "length") {
    throw new Error("AI_TRUNCATED_RESPONSE: yanıt kesildi");
  }
  return content.trim();
}

/** İçerik/prompt loglamadan yalnızca güvenli tamamlama telemetrisi. */
function logCompletion(
  phase: "provider_request_completed",
  model: string,
  startedAt: number,
  response: ChatCompletion
): void {
  const choice = response.choices?.[0];
  const content = choice?.message?.content;
  console.info(phase, {
    model,
    provider_duration_ms: Date.now() - startedAt,
    provider_timeout: false,
    choices_count: response.choices?.length ?? 0,
    finish_reason: choice?.finish_reason ?? null,
    content_length: typeof content === "string" ? content.length : 0
  });
}

function logProviderFailure(model: string, startedAt: number, err: unknown): void {
  const { category, httpStatus } = classifyProviderError(err);
  console.error("provider_request_completed", {
    model,
    provider_duration_ms: Date.now() - startedAt,
    provider_timeout: category === "PROVIDER_TIMEOUT",
    provider_error_category: category,
    http_status: httpStatus
  });
}

export class ZaiProvider implements AiProvider {
  private client?: OpenAI;

  constructor(private readonly clientFactory: () => OpenAI = createZaiClient) {}

  private getClient(): OpenAI {
    if (!this.client) {
      this.client = this.clientFactory();
    }
    return this.client;
  }

  async generateText(input: GenerateTextInput): Promise<string> {
    const messages: ChatMessage[] = [
      { role: "system", content: input.system },
      ...input.turns.map((turn) => ({ role: turn.role, content: turn.content } as ChatMessage))
    ];
    const startedAt = Date.now();
    console.info("provider_request_started", { model: TEXT_MODEL, json: Boolean(input.json) });
    try {
      const response = await this.getClient().chat.completions.create(
        {
          model: TEXT_MODEL,
          messages,
          max_tokens: input.maxTokens,
          ...(input.json ? { response_format: { type: "json_object" as const } } : {})
        },
        { timeout: input.timeoutMs ?? TEXT_REQUEST_TIMEOUT_MS, maxRetries: AI_MAX_RETRIES }
      );
      const text = extractCompletionText(response);
      logCompletion("provider_request_completed", TEXT_MODEL, startedAt, response);
      return text;
    } catch (err) {
      logProviderFailure(TEXT_MODEL, startedAt, err);
      throw err;
    }
  }

  async analyzeImages(input: AnalyzeImagesInput): Promise<string> {
    const messages: ChatMessage[] = [
      { role: "system", content: input.system },
      { role: "user", content: buildVisionContent(input.images, input.prompt) }
    ];
    const startedAt = Date.now();
    console.info("provider_request_started", { model: VISION_MODEL, images: input.images.length });
    try {
      // Vision modelleri response_format desteklemez; JSON yalnızca prompt ile istenir.
      const response = await this.getClient().chat.completions.create(
        {
          model: VISION_MODEL,
          messages,
          max_tokens: input.maxTokens
        },
        { timeout: input.timeoutMs ?? VISION_REQUEST_TIMEOUT_MS, maxRetries: AI_MAX_RETRIES }
      );
      const text = extractCompletionText(response);
      logCompletion("provider_request_completed", VISION_MODEL, startedAt, response);
      return text;
    } catch (err) {
      logProviderFailure(VISION_MODEL, startedAt, err);
      throw err;
    }
  }
}

let provider: AiProvider | undefined;

/** Yalnızca başarılı oluşturma önbelleğe alınır; istek verisi asla saklanmaz. */
export function getAiProvider(): AiProvider {
  if (!provider) {
    provider = new ZaiProvider();
  }
  return provider;
}
