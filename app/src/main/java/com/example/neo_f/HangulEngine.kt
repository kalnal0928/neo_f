package com.example.neo_f

class HangulEngine(
    private val listener: (Result) -> Unit,
    var syllableTimeoutMs: Long = 300L,
    var isCharacterCycleEnabled: Boolean = true
) {

    // 초성, 중성, 종성 리스트
    private val CHOSEONG_LIST = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
    private val JUNGSEONG_LIST = listOf('ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ')
    private val JONGSEONG_LIST = listOf(' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')

    private val JUNGSEONG_COMBINATIONS = mapOf(
        ('ㅗ' to 'ㅏ') to 'ㅘ', ('ㅗ' to 'ㅐ') to 'ㅙ', ('ㅗ' to 'ㅣ') to 'ㅚ',
        ('ㅜ' to 'ㅓ') to 'ㅝ', ('ㅜ' to 'ㅔ') to 'ㅞ', ('ㅜ' to 'ㅣ') to 'ㅟ',
        ('ㅡ' to 'ㅣ') to 'ㅢ',
        ('ㅏ' to 'ㅣ') to 'ㅐ', ('ㅓ' to 'ㅣ') to 'ㅔ',
        ('ㅣ' to 'ㅏ') to 'ㅑ', ('ㅣ' to 'ㅓ') to 'ㅕ', ('ㅣ' to 'ㅗ') to 'ㅛ', ('ㅣ' to 'ㅜ') to 'ㅠ'
    )
    
    private val VOWEL_DOUBLE_TO_Y = mapOf('ㅏ' to 'ㅑ', 'ㅓ' to 'ㅕ', 'ㅗ' to 'ㅛ', 'ㅜ' to 'ㅠ')
    
    private val JONGSEONG_COMBINATIONS = mapOf(
        ('ㄱ' to 'ㅅ') to 'ㄳ', ('ㄴ' to 'ㅈ') to 'ㄵ', ('ㄴ' to 'ㅎ') to 'ㄶ',
        ('ㄹ' to 'ㄱ') to 'ㄺ', ('ㄹ' to 'ㅁ') to 'ㄻ', ('ㄹ' to 'ㅂ') to 'ㄼ',
        ('ㄹ' to 'ㅅ') to 'ㄽ', ('ㄹ' to 'ㅌ') to 'ㄾ', ('ㄹ' to 'ㅍ') to 'ㄿ',
        ('ㄹ' to 'ㅎ') to 'ㅀ', ('ㅂ' to 'ㅅ') to 'ㅄ'
    )
    
    private val JONGSEONG_SPLIT = mapOf(
        'ㄳ' to ('ㄱ' to 'ㅅ'), 'ㄵ' to ('ㄴ' to 'ㅈ'), 'ㄶ' to ('ㄴ' to 'ㅎ'),
        'ㄺ' to ('ㄹ' to 'ㄱ'), 'ㄻ' to ('ㄹ' to 'ㅁ'), 'ㄼ' to ('ㄹ' to 'ㅂ'),
        'ㄽ' to ('ㄹ' to 'ㅅ'), 'ㄾ' to ('ㄹ' to 'ㅌ'), 'ㄿ' to ('ㄹ' to 'ㅍ'),
        'ㅀ' to ('ㄹ' to 'ㅎ'), 'ㅄ' to ('ㅂ' to 'ㅅ'), 'ㄲ' to ('ㄱ' to 'ㄱ'), 'ㅆ' to ('ㅅ' to 'ㅅ')
    )
    
    private val DOUBLE_CONSONANTS = mapOf('ㄱ' to 'ㄲ', 'ㄷ' to 'ㄸ', 'ㅂ' to 'ㅃ', 'ㅅ' to 'ㅆ', 'ㅈ' to 'ㅉ')
    
    private val CONSONANT_CYCLE = mapOf(
        'ㄱ' to listOf('ㄱ', 'ㅋ', 'ㄲ'), 'ㅋ' to listOf('ㄱ', 'ㅋ', 'ㄲ'), 'ㄲ' to listOf('ㄱ', 'ㅋ', 'ㄲ'),
        'ㄴ' to listOf('ㄴ'),
        'ㄷ' to listOf('ㄷ', 'ㅌ', 'ㄸ'), 'ㅌ' to listOf('ㄷ', 'ㅌ', 'ㄸ'), 'ㄸ' to listOf('ㄷ', 'ㅌ', 'ㄸ'),
        'ㄹ' to listOf('ㄹ'),
        'ㅁ' to listOf('ㅁ', 'ㅂ'),
        'ㅂ' to listOf('ㅂ', 'ㅍ', 'ㅃ'), 'ㅍ' to listOf('ㅂ', 'ㅍ', 'ㅃ'), 'ㅃ' to listOf('ㅂ', 'ㅍ', 'ㅃ'),
        'ㅅ' to listOf('ㅅ', 'ㅆ'), 'ㅆ' to listOf('ㅅ', 'ㅆ'),
        'ㅇ' to listOf('ㅇ', 'ㅎ'), 'ㅎ' to listOf('ㅇ', 'ㅎ'),
        'ㅈ' to listOf('ㅈ', 'ㅊ', 'ㅉ'), 'ㅊ' to listOf('ㅈ', 'ㅊ', 'ㅉ'), 'ㅉ' to listOf('ㅈ', 'ㅊ', 'ㅉ')
    )
    
    private val VOWEL_CYCLE = mapOf(
        'ㅏ' to listOf('ㅏ', 'ㅑ'), 'ㅑ' to listOf('ㅏ', 'ㅑ'),
        'ㅓ' to listOf('ㅓ', 'ㅕ'), 'ㅕ' to listOf('ㅓ', 'ㅕ'),
        'ㅗ' to listOf('ㅗ', 'ㅛ'), 'ㅛ' to listOf('ㅗ', 'ㅛ'),
        'ㅜ' to listOf('ㅜ', 'ㅠ'), 'ㅠ' to listOf('ㅜ', 'ㅠ'),
        'ㅡ' to listOf('ㅡ'), 'ㅣ' to listOf('ㅣ')
    )

    private var currentState = State()
    private var lastKey: Char? = null
    private var lastKeyTime: Long = 0

    data class State(var choseong: Char? = null, var jungseong: Char? = null, var jongseong: Char? = null)
    data class Result(val committed: String = "", val composing: String = "")
    
    fun applyDoubleConsonant() {
        var changed = false
        if (currentState.jongseong != null && currentState.jongseong in DOUBLE_CONSONANTS.keys) {
            currentState.jongseong = DOUBLE_CONSONANTS[currentState.jongseong]
            changed = true
        } else if (currentState.choseong != null && currentState.jungseong == null && currentState.choseong in DOUBLE_CONSONANTS.keys) {
            currentState.choseong = DOUBLE_CONSONANTS[currentState.choseong]
            changed = true
        }
        if (changed) {
            listener(Result(composing = combineHangul()))
        }
    }

    fun processKey(key: Char) {
        val currentTime = System.currentTimeMillis()
        
        if (key == ' ') {
            val committed = commitCurrentState()
            listener(Result(committed + " ", ""))
            lastKey = key
            lastKeyTime = currentTime
            return
        }
        
        val isConsonant = key in CHOSEONG_LIST
        val isVowel = key in JUNGSEONG_LIST
        val timeSinceLastKey = currentTime - lastKeyTime
        var committed = ""

        // 순환 처리
        if (isCharacterCycleEnabled && lastKey == key && timeSinceLastKey < syllableTimeoutMs) {
            var cycled = false
            
            if (isConsonant) {
                val cycle = CONSONANT_CYCLE[key]
                if (cycle != null && cycle.size > 1) {
                    if (currentState.jongseong != null && currentState.jongseong in cycle) {
                        val currentIndex = cycle.indexOf(currentState.jongseong)
                        val nextIndex = (currentIndex + 1) % cycle.size
                        currentState.jongseong = cycle[nextIndex]
                        cycled = true
                    } else if (currentState.choseong != null && currentState.jungseong == null && currentState.choseong in cycle) {
                        val currentIndex = cycle.indexOf(currentState.choseong)
                        val nextIndex = (currentIndex + 1) % cycle.size
                        currentState.choseong = cycle[nextIndex]
                        cycled = true
                    }
                }
            } else if (isVowel) {
                val cycle = VOWEL_CYCLE[key]
                if (cycle != null && cycle.size > 1) {
                    if (currentState.jungseong != null && currentState.jungseong in cycle && currentState.jongseong == null) {
                        val currentIndex = cycle.indexOf(currentState.jungseong)
                        val nextIndex = (currentIndex + 1) % cycle.size
                        currentState.jungseong = cycle[nextIndex]
                        cycled = true
                    }
                }
            }

            if (cycled) {
                lastKey = key
                lastKeyTime = currentTime
                listener(Result(composing = combineHangul()))
                return
            }
        }

        if (isConsonant) {
            if (currentState.jungseong != null) {
                if (currentState.jongseong == null) {
                    if (JONGSEONG_LIST.contains(key)) {
                        currentState.jongseong = key
                    } else {
                        committed = commitCurrentState()
                        currentState.choseong = key
                    }
                } else {
                    val newJongseong = JONGSEONG_COMBINATIONS[currentState.jongseong to key]
                    if (newJongseong != null) {
                        currentState.jongseong = newJongseong
                    } else {
                        if (isCharacterCycleEnabled && lastKey == key && timeSinceLastKey < syllableTimeoutMs) {
                            val cycle = CONSONANT_CYCLE[key]
                            if (cycle != null && cycle.size > 1) {
                                val nextIndex = 1 % cycle.size
                                val keyToUse = cycle[nextIndex]
                                val newJongseongCycle = JONGSEONG_COMBINATIONS[currentState.jongseong to keyToUse]
                                if (newJongseongCycle != null) {
                                    currentState.jongseong = newJongseongCycle
                                    lastKey = key
                                    lastKeyTime = currentTime
                                    listener(Result(composing = combineHangul()))
                                    return
                                }
                            }
                        }
                        committed = commitCurrentState()
                        currentState.choseong = key
                    }
                }
            } else {
                if (currentState.choseong != null && timeSinceLastKey > syllableTimeoutMs) {
                    committed += currentState.choseong.toString()
                    currentState = State()
                    currentState.choseong = key
                } else if (currentState.choseong != null) {
                    committed += currentState.choseong.toString()
                    currentState = State()
                    currentState.choseong = key
                } else {
                    currentState.choseong = key
                }
            }
        } else if (isVowel) {
            if (currentState.jongseong != null) {
                val split = JONGSEONG_SPLIT[currentState.jongseong]
                val lastJongseong = currentState.jongseong
                currentState.jongseong = null
                if (split != null) {
                    val (first, second) = split
                    currentState.jongseong = first
                    committed = commitCurrentState()
                    currentState.choseong = second
                    currentState.jungseong = key
                } else {
                    committed = commitCurrentState()
                    currentState.choseong = lastJongseong
                    currentState.jungseong = key
                }
            } else if (currentState.choseong != null) {
                if (currentState.jungseong != null) {
                    if (currentState.jungseong == key && timeSinceLastKey < syllableTimeoutMs && key in VOWEL_DOUBLE_TO_Y.keys) {
                        currentState.jungseong = VOWEL_DOUBLE_TO_Y[key]!!
                    } else {
                        val newJungseong = JUNGSEONG_COMBINATIONS[currentState.jungseong to key]
                        if (newJungseong != null) {
                            currentState.jungseong = newJungseong
                        } else {
                            committed = commitCurrentState()
                            currentState.choseong = 'ㅇ'
                            currentState.jungseong = key
                        }
                    }
                } else {
                    currentState.jungseong = key
                }
            } else {
                committed += key.toString()
            }
        }

        lastKey = key
        lastKeyTime = currentTime
        listener(Result(committed, combineHangul()))
    }

    private fun commitCurrentState(): String {
        val combined = combineHangul()
        reset()
        return combined
    }

    fun backspace(): String {
        lastKey = null
        lastKeyTime = 0

        if (currentState.jongseong != null) {
            val originalJongseong = currentState.jongseong
            currentState.jongseong = null
            JONGSEONG_COMBINATIONS.entries.find { it.value == originalJongseong }?.let { currentState.jongseong = it.key.first }
        } else if (currentState.jungseong != null) {
            val originalJungseong = currentState.jungseong
            currentState.jungseong = null
            JUNGSEONG_COMBINATIONS.entries.find { it.value == originalJungseong }?.let { currentState.jungseong = it.key.first }
        } else if (currentState.choseong != null) {
            currentState.choseong = null
        } else {
            return "BACKSPACE"
        }
        return combineHangul()
    }

    fun reset() {
        currentState = State()
        lastKey = null
        lastKeyTime = 0
    }

    private fun combineHangul(): String {
        val (choseong, jungseong, jongseong) = currentState
        if (choseong == null && jungseong == null && jongseong == null) return ""
        if (jungseong == null) return choseong?.toString() ?: ""

        val choseongIndex = CHOSEONG_LIST.indexOf(choseong ?: 'ㅇ')
        val jungseongIndex = JUNGSEONG_LIST.indexOf(jungseong)
        val jongseongIndex = JONGSEONG_LIST.indexOf(jongseong ?: ' ')

        if (choseongIndex == -1 || jungseongIndex == -1 || jongseongIndex == -1) {
            return "${choseong ?: ""}${jungseong}${jongseong ?: ""}"
        }

        val unicode = 0xAC00 + (choseongIndex * 21 * 28) + (jungseongIndex * 28) + jongseongIndex
        return unicode.toChar().toString()
    }
}
