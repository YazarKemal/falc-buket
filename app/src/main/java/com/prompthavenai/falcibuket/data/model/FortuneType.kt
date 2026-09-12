package com.prompthavenai.falcibuket.data.model

/**
 * The 28 supported reading types. Identity is the stable ASCII [id]; UI text is
 * Turkish. Artwork is resolved through the central [FortuneArtwork] mapping so
 * new `category_*` assets only require one mapping update.
 */
enum class FortuneType(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: FortuneCategory,
    val inputMode: InputMode,
    val aiMode: AiMode,
    val isPremium: Boolean,
    val memoryEnabled: Boolean,
    val historyEnabled: Boolean
) {
    // Popular
    COFFEE(
        "coffee", "Kahve Falı", "Fincanındaki işaretleri keşfet",
        FortuneCategory.POPULAR, InputMode.PHOTO_VISION, AiMode.VISION,
        isPremium = false, memoryEnabled = true, historyEnabled = true
    ),
    TAROT(
        "tarot", "Tarot", "Kartların sana ne söylüyor?",
        FortuneCategory.CARDS, InputMode.CARD_SELECTION, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = true, historyEnabled = true
    ),
    LOVE(
        "love", "Aşk Falı", "Kalbindeki sorulara bak",
        FortuneCategory.POPULAR, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = true, historyEnabled = true
    ),
    CAREER_MONEY(
        "career_money", "Kariyer & Para", "Önündeki fırsatları keşfet",
        FortuneCategory.POPULAR, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = true, historyEnabled = true
    ),

    // Cards
    KATINA(
        "katina", "Katina Falı", "Kartlarla aşk ve kader yorumu",
        FortuneCategory.CARDS, InputMode.CARD_SELECTION, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    PLAYING_CARDS(
        "playing_cards", "İskambil Falı", "İskambil kartlarının mesajı",
        FortuneCategory.CARDS, InputMode.CARD_SELECTION, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    LENORMAND(
        "lenormand", "Lenormand", "36 kartlık klasik kehanet",
        FortuneCategory.CARDS, InputMode.CARD_SELECTION, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    ORACLE(
        "oracle", "Oracle Kartları", "Sezgisel kart yorumu",
        FortuneCategory.CARDS, InputMode.CARD_SELECTION, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),

    // Astrology & Numbers
    BIRTH_CHART(
        "birth_chart", "Doğum Haritası", "Gökyüzü senin doğumunda ne diyordu?",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    DAILY_HOROSCOPE(
        "daily_horoscope", "Günlük Burç", "Bugünün gökyüzü mesajı",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    COMPATIBILITY(
        "compatibility", "Uyumluluk", "İki kalbin gökyüzü uyumu",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.TWO_PERSON_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    NUMEROLOGY(
        "numerology", "Numeroloji", "İsminin ve sayılarının sırrı",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    CHINESE_ZODIAC(
        "chinese_zodiac", "Çin Burcu", "Doğduğun yılın hayvanı",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    MOON_READING(
        "moon_reading", "Ay Burcu", "Ay'ın sana anlattıkları",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    BIORHYTHM(
        "biorhythm", "Bioritim", "Bedeninin ve zihninin ritmi",
        FortuneCategory.ASTROLOGY_NUMBERS, InputMode.BIRTH_DATA, AiMode.STRUCTURED,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),

    // Ancient Methods
    RUNES(
        "runes", "Rün Falı", "Kadim kuzey sembolleri",
        FortuneCategory.ANCIENT_METHODS, InputMode.SYMBOL_SELECTION, AiMode.CAST,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    I_CHING(
        "i_ching", "I Ching", "Değişim kitabının bilgeliği",
        FortuneCategory.ANCIENT_METHODS, InputMode.RANDOM_CAST, AiMode.CAST,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    PALM(
        "palm", "El Falı", "Avucundaki çizgileri oku",
        FortuneCategory.ANCIENT_METHODS, InputMode.PHOTO_VISION, AiMode.VISION,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    DREAM(
        "dream", "Rüya Tabiri", "Rüyanı anlat, sembolleri çözelim",
        FortuneCategory.ANCIENT_METHODS, InputMode.TEXT_INPUT, AiMode.TEXT,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    DAISY(
        "daisy", "Papatya Falı", "Seviyor, sevmiyor...",
        FortuneCategory.ANCIENT_METHODS, InputMode.RANDOM_CAST, AiMode.CAST,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    DICE(
        "dice", "Zar Falı", "Zarın gösterdiği yol",
        FortuneCategory.ANCIENT_METHODS, InputMode.RANDOM_CAST, AiMode.CAST,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    TEA_LEAF(
        "tea_leaf", "Çay Yaprağı", "Fincan yerine yapraklara bak",
        FortuneCategory.ANCIENT_METHODS, InputMode.PHOTO_VISION, AiMode.VISION,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    CANDLE_WAX(
        "candle_wax", "Mum Falı", "Mumun bıraktığı izler",
        FortuneCategory.ANCIENT_METHODS, InputMode.PHOTO_VISION, AiMode.VISION,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),

    // Energy & Intention
    PENDULUM(
        "pendulum", "Sarkaç", "Evet, hayır ya da belki",
        FortuneCategory.ENERGY_INTENTION, InputMode.SYMBOL_SELECTION, AiMode.CAST,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    AURA(
        "aura", "Aura", "Enerji alanının rengi",
        FortuneCategory.ENERGY_INTENTION, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    CHAKRA(
        "chakra", "Çakra", "Enerji merkezlerinin dengesi",
        FortuneCategory.ENERGY_INTENTION, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),

    // Other
    CRYSTAL_BALL(
        "crystal_ball", "Kristal Küre", "Berrak küreye bak",
        FortuneCategory.OTHER, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    ),
    CLAIRVOYANCE(
        "clairvoyance", "Durugörü", "Sezgilerin sana ne söylüyor?",
        FortuneCategory.OTHER, InputMode.QUESTION_ONLY, AiMode.TEXT,
        isPremium = false, memoryEnabled = false, historyEnabled = false
    );

    /** Catalog card artwork from the central mapping. */
    val artworkRes: Int get() = FortuneArtwork.categoryRes(this)

    /** Result header artwork + aspect ratio from the central mapping. */
    val resultArtwork: FortuneArtworkSpec get() = FortuneArtwork.spec(this)
}
