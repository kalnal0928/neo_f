package com.example.neo_f

class HangulEngine(
    private val listener: (Result) -> Unit,
    var syllableTimeoutMs: Long = 300L  // 음절 조합 타이머 (기본 300ms)
) {

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
    
    // 된소리 변환 (ㄲ 버튼으로 변환)
    private val DOUBLE_CONSONANTS = mapOf('ㄱ' to 'ㄲ', 'ㄷ' to 'ㄸ', 'ㅂ' to 'ㅃ', 'ㅅ' to 'ㅆ', 'ㅈ' to 'ㅉ')
    
    // 특수 조합 (ㅅㅅ → ㅆ, ㅁㅁ → ㅂ)
    private val SPECIAL_COMBINATIONS = mapOf('ㅅ' to 'ㅆ', 'ㅁ' to 'ㅂ')

    // 상태 변수
    private var currentState = State()
    private var lastKey: Char? = null
    private var lastKeyTime: Long = 0

    data class State(var choseong: Char? = null, var jungseong: Char? = null, var jongseong: Char? = null)
    data class Result(val committed: String = "", val composing: String = "")
    
    // 된소리 변환 함수 (ㄲ 버튼 전용)
    fun applyDoubleConsonant() {
        var changed = false
        
        // 종성이 있으면 종성을 된소리로 변환
        if (currentState.jongseong != null && currentState.jongseong in DOUBLE_CONSONANTS.keys) {
            currentState.jongseong = DOUBLE_CONSONANTS[currentState.jongseong]
            changed = true
        }
        // 초성만 있으면 초성을 된소리로 변환
        else if (currentState.choseong != null && currentState.jungseong == null && currentState.choseong in DOUBLE_CONSONANTS.keys) {
            currentState.choseong = DOUBLE_CONSONANTS[currentState.choseong]
            changed = true
        }
        
        if (changed) {
            listener(Result(composing = combineHangul()))
        }
    }

    fun processKey(key: Char) {
        val currentTime = System.currentTimeMillis()
        val isConsonant = key in CHOSEONG_LIST
        val isVowel = key in JUNGSEONG_LIST
        val timeSinceLastKey = currentTime - lastKeyTime
        var committed = ""

        // 같은 키를 타이머 내에 두 번 눌렀을 때 특수 변환
        if (lastKey == key && timeSinceLastKey < syllableTimeoutMs) {
            var replaced = false
            
            // 1. 격음 변환 (ㄱㄱ → ㅋ, ㄷㄷ → ㅌ, ㅂㅂ → ㅍ, ㅈㅈ → ㅊ)
            if (key in ASPIRATED_CONSONANTS.keys) {
                val aspiratedKey = ASPIRATED_CONSONANTS[key]!!
                
                if (currentState.jongseong == key) {
                    currentState.jongseong = aspiratedKey
                    replaced = true
                } else if (currentState.choseong == key && currentState.jungseong == null) {
                    currentState.choseong = aspiratedKey
                    replaced = true
                }
            }
            // 2. 특수 조합 (ㅅㅅ → ㅆ, ㅁㅁ → ㅂ)
            else if (key in SPECIAL_COMBINATIONS.keys) {
                val specialKey = SPECIAL_COMBINATIONS[key]!!
                
                if (currentState.jongseong == key) {
                    currentState.jongseong = specialKey
                    replaced = true
                } else if (currentState.choseong == key && currentState.jungseong == null) {
                    currentState.choseong = specialKey
                    replaced = true
                }
            }

            if (replaced) {
                // 변환 후에도 원래 키를 lastKey로 유지 (다음 변환을 위해)
                lastKey = key
                lastKeyTime = currentTime
                listener(Result(composing = combineHangul()))
                return
            }
        }

        lastKey = key
        lastKeyTime = currentTime

        if (isConsonant) {
            if (currentState.jungseong != null) {
                // 중성이 있는 경우 - 종성으로 추가 시도
                val newJongseong = JONGSEONG_COMBINATIONS[currentState.jongseong to key]
                if (currentState.jongseong == null && JONGSEONG_LIST.contains(key)) {
                    currentState.jongseong = key
                } else if (newJongseong != null) {
                    currentState.jongseong = newJongseong
                } else {
                    // 종성 조합 불가 - 현재 음절 완성하고 새 초성 시작
                    committed = commitCurrentState()
                    currentState.choseong = key
                }
            } else {
                // 중성이 없는 경우
                if (currentState.choseong != null && timeSinceLastKey > syllableTimeoutMs) {
                    // 타이머 초과 시 이전 자음을 바로 커밋하고 새 초성 시작
                    committed += currentState.choseong.toString()
                    currentState = State() // reset() 대신 상태만 초기화
                    currentState.choseong = key
                    // lastKey와 lastKeyTime은 이미 위에서 설정됨
                } else if (currentState.choseong != null) {
                    // 타이머 내에 자음이 연속으로 오면 이전 자음 커밋
                    committed += currentState.choseong.toString()
                    currentState = State() // reset() 대신 상태만 초기화
                    currentState.choseong = key
                    // lastKey와 lastKeyTime은 이미 위에서 설정됨
                } else {
                    // 아무것도 없는 경우 - 새 초성 시작
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
                 val newJungseong = JUNGSEONG_COMBINATIONS[currentState.jungseong to key]
                 if (currentState.jungseong != null && newJungseong != null) {
                    currentState.jungseong = newJungseong
                } else {
                    currentState.jungseong = key
                }
            } else {
                // 초성 없이 모음만 입력하는 경우 - 모음을 바로 커밋
                committed += key.toString()
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