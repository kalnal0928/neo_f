package com.example.neo_f

class HangulEngine(private val listener: (Result) -> Unit) {

    // 초성, 중성, 종성 리스트
    private val CHOSEONG_LIST = listOf('ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')
    private val JUNGSEONG_LIST = listOf('ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ')
    private val JONGSEONG_LIST = listOf(' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ')

    // 조합 규칙
    private val JUNGSEONG_COMBINATIONS = mapOf(
        ('ㅗ' to 'ㅏ') to 'ㅘ', ('ㅗ' to 'ㅐ') to 'ㅙ', ('ㅗ' to 'ㅣ') to 'ㅚ',
        ('ㅜ' to 'ㅓ') to 'ㅝ', ('ㅜ' to 'ㅔ') to 'ㅞ', ('ㅜ' to 'ㅣ') to 'ㅟ',
        ('ㅡ' to 'ㅣ') to 'ㅢ',
        ('ㅏ' to 'ㅣ') to 'ㅐ', ('ㅓ' to 'ㅣ') to 'ㅔ',
        ('ㅣ' to 'ㅏ') to 'ㅑ', ('ㅣ' to 'ㅓ') to 'ㅕ', ('ㅣ' to 'ㅗ') to 'ㅛ', ('ㅣ' to 'ㅜ') to 'ㅠ'
    )
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
    private val ASPIRATED_CONSONANTS = mapOf('ㄱ' to 'ㅋ', 'ㄷ' to 'ㅌ', 'ㅂ' to 'ㅍ', 'ㅈ' to 'ㅊ', 'ㅇ' to 'ㅎ')

    // 상태 변수
    private var currentState = State()
    private var lastConsonant: Char? = null
    private var lastConsonantTime: Long = 0
    private val DOUBLE_TAP_THRESHOLD_MS = 300L

    data class State(var choseong: Char? = null, var jungseong: Char? = null, var jongseong: Char? = null)
    data class Result(val committed: String = "", val composing: String = "")

    fun processKey(key: Char) {
        val currentTime = System.currentTimeMillis()
        val isConsonant = key in CHOSEONG_LIST
        val isVowel = key in JUNGSEONG_LIST
        val isDoubleTappable = key in ASPIRATED_CONSONANTS.keys
        var committed = ""

        if (isDoubleTappable && lastConsonant == key && (currentTime - lastConsonantTime) < DOUBLE_TAP_THRESHOLD_MS) {
            val aspiratedKey = ASPIRATED_CONSONANTS[key]!!
            var replaced = false
            if (currentState.jongseong == key) {
                currentState.jongseong = aspiratedKey
                replaced = true
            } else if (currentState.choseong == key && currentState.jungseong == null) {
                currentState.choseong = aspiratedKey
                replaced = true
            }

            if (replaced) {
                lastConsonant = null
                lastConsonantTime = 0
                listener(Result(composing = combineHangul()))
                return
            }
        }

        if (isConsonant) {
            lastConsonant = key
            lastConsonantTime = currentTime
        } else {
            lastConsonant = null
            lastConsonantTime = 0
        }

        if (isConsonant) {
            if (currentState.jungseong != null) {
                val newJongseong = JONGSEONG_COMBINATIONS[currentState.jongseong to key]
                if (currentState.jongseong == null && JONGSEONG_LIST.contains(key)) {
                    currentState.jongseong = key
                } else if (newJongseong != null) {
                    currentState.jongseong = newJongseong
                } else {
                    committed = commitCurrentState()
                    currentState.choseong = key
                }
            } else {
                if (currentState.choseong != null) {
                    committed = commitCurrentState()
                }
                currentState.choseong = key
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
                 val newJungseong = JUNGSEONG_COMBINATIONS[currentState.jungseong to key]
                 if (currentState.jungseong != null && newJungseong != null) {
                    currentState.jungseong = newJungseong
                } else {
                    currentState.jungseong = key
                }
            } else {
                committed = commitCurrentState()
                currentState.jungseong = key
            }
        }

        listener(Result(committed, combineHangul()))
    }

    private fun commitCurrentState(): String {
        val combined = combineHangul()
        reset()
        return combined
    }

    fun backspace(): String {
        lastConsonant = null
        lastConsonantTime = 0

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
        lastConsonant = null
        lastConsonantTime = 0
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