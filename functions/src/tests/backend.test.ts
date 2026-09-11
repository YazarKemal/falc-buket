import { test } from "node:test";
import assert from "node:assert/strict";
import { mergeMemories, buildMemoryBlock, MemoryRecord } from "../memory";
import { validateCoffeeResult, parseMemoryCandidates, validateImageData } from "../schemas";

test("mergeMemories creates new memories", () => {
  const { toCreate, toUpdate } = mergeMemories(
    [],
    [{ category: "career", subject: "iş başvurusu", fact: "Kullanıcı bir iş başvurusundan cevap bekliyor.", importance: 0.8, confidence: 0.9 }]
  );
  assert.equal(toCreate.length, 1);
  assert.equal(toUpdate.length, 0);
});

test("mergeMemories deduplicates same subject case-insensitively", () => {
  const existing: MemoryRecord[] = [
    { id: "m1", category: "relationship", subject: "Ayşe", fact: "Kullanıcı Ayşe ile yakın zamanda ayrıldı.", importance: 0.9, confidence: 0.95 }
  ];
  const { toCreate, toUpdate } = mergeMemories(
    existing,
    [{ category: "relationship", subject: "ayşe", fact: "Kullanıcı Ayşe ile ilişkisi hakkında düşünüyor.", importance: 0.85, confidence: 0.9 }]
  );
  assert.equal(toCreate.length, 0);
  assert.equal(toUpdate.length, 1);
  assert.equal(toUpdate[0].id, "m1");
});

test("mergeMemories keeps existing fact when incoming is weaker", () => {
  const existing: MemoryRecord[] = [
    { id: "m1", category: "goal", subject: "sınav", fact: "Kullanıcı önemli bir sınav hazırlığında.", importance: 0.95, confidence: 0.97 }
  ];
  const { toCreate, toUpdate } = mergeMemories(
    existing,
    [{ category: "goal", subject: "sınav", fact: "Kullanıcı belki bir sınava giriyor olabilir.", importance: 0.3, confidence: 0.4 }]
  );
  assert.equal(toUpdate.length, 0);
});

test("mergeMemories updates when fact meaningfully changed and stronger", () => {
  const existing: MemoryRecord[] = [
    { id: "m1", category: "career", subject: "iş", fact: "Kullanıcı iş arıyor.", importance: 0.8, confidence: 0.9 }
  ];
  const { toCreate, toUpdate } = mergeMemories(
    existing,
    [{ category: "career", subject: "İŞ", fact: "Kullanıcı iki teklif arasında karar veriyor.", importance: 0.9, confidence: 0.95 }]
  );
  assert.equal(toUpdate.length, 1);
  assert.equal(toUpdate[0].fact, "Kullanıcı iki teklif arasında karar veriyor.");
});

test("buildMemoryBlock handles empty memory", () => {
  const block = buildMemoryBlock([]);
  assert.match(block, /hafıza bilgisi henüz yok/);
});

test("buildMemoryBlock lists facts", () => {
  const block = buildMemoryBlock([{ category: "career", subject: "iş", fact: "Kullanıcı iş arıyor.", importance: 0.9, confidence: 0.9 }]);
  assert.match(block, /Kullanıcı iş arıyor/);
});

const validCoffee = {
  title: "Yolun Başındaki Kuş",
  summary: "Fincanında hareketli bir dönem işaret ediyor.",
  visualObservations: [{ observation: "Sağ üstte kuş benzeri şekil", interpretation: "Haber işareti" }],
  generalEnergy: "Enerji hareketli.",
  love: "İletişim öne çıkıyor.",
  careerMoney: "Fırsat beliriyor.",
  nearFuture: "Kısa sürede netleşme.",
  highlight: "Bir haber bekleniyor.",
  timeWindows: [{ topic: "İş", window: "önümüzdeki 2-3 hafta" }],
  memoryCandidates: []
};

test("validateCoffeeResult accepts full valid result", () => {
  const parsed = validateCoffeeResult(validCoffee);
  assert.equal(parsed.title, "Yolun Başındaki Kuş");
  assert.equal(parsed.visualObservations.length, 1);
});

test("validateCoffeeResult rejects missing required field", () => {
  const bad = { ...validCoffee } as Record<string, unknown>;
  delete bad.highlight;
  assert.throws(() => validateCoffeeResult(bad), /highlight/);
});

test("validateCoffeeResult rejects non-object", () => {
  assert.throws(() => validateCoffeeResult("nope"), /obje değil/);
});

test("validateCoffeeResult filters invalid observations", () => {
  const withBad = {
    ...validCoffee,
    visualObservations: [
      { observation: "   ", interpretation: "x" },
      { observation: "Uzun bir yol", interpretation: "" }
    ]
  };
  const parsed = validateCoffeeResult(withBad);
  assert.equal(parsed.visualObservations.length, 1);
});

test("parseMemoryCandidates drops invalid category and scores", () => {
  const out = parseMemoryCandidates({
    memories: [
      { category: "hacker", subject: "x", fact: "bu bir geçerli cümledir", importance: 0.5, confidence: 0.5 },
      { category: "career", subject: "iş", fact: "kısa", importance: 0.5, confidence: 0.5 },
      { category: "career", subject: "iş", fact: "Geçerli bir memory cümlesi.", importance: 2, confidence: 0.5 },
      { category: "goal", subject: "taşınma", fact: "Kullanıcı yurt dışına taşınmayı planlıyor.", importance: 0.7, confidence: 0.8 }
    ]
  });
  assert.equal(out.length, 1);
  assert.equal(out[0].subject, "taşınma");
});

// JPEG magic bytes (FFD8FF) içeren minimal geçerli base64
const jpegBase64 = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]).toString("base64");
const pngBase64 = Buffer.from([0x89, 0x50, 0x4e, 0x47, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]).toString("base64");
const textBase64 = Buffer.from("merhaba dünya bu bir görsel değil").toString("base64");

test("validateImageData accepts jpeg and png", () => {
  assert.equal(validateImageData(jpegBase64, 100000).mime, "image/jpeg");
  assert.equal(validateImageData(pngBase64, 100000).mime, "image/jpeg"); // sniff: PNG kabul, default mime
  assert.equal(validateImageData(`data:image/png;base64,${pngBase64}`, 100000).mime, "image/png");
});

test("validateImageData rejects non-image", () => {
  assert.throws(() => validateImageData(textBase64, 100000), /desteklenmeyen format/);
});

test("validateImageData rejects oversized payload", () => {
  const big = "A".repeat(10_000_000);
  assert.throws(() => validateImageData(big, 1_000_000), /IMAGE_TOO_LARGE/);
});

test("validateImageData rejects malformed base64", () => {
  assert.throws(() => validateImageData("!!!geçersiz!!!", 100000), /IMAGE_INVALID/);
});
