package com.swan1127.repland.domain.model

object PersonalProfileName {
    const val Default = "Repland 用户"
    const val MaxLength = 24

    fun length(value: String) = value.codePointCount(0, value.length)

    fun error(value: String): String? = when {
        value.any { it.isISOControl() || it == '\u2028' || it == '\u2029' } -> "用户名不能包含换行或控制字符"
        value.trim().isBlank() -> "请输入用户名"
        length(value.trim()) > MaxLength -> "用户名最多 24 个字"
        else -> null
    }

    fun normalized(value: String): String {
        require(error(value) == null) { error(value).orEmpty() }
        return value.trim()
    }
}
