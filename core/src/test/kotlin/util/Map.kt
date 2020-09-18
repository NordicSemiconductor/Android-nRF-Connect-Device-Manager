package util

inline fun <reified T> Map<String, Any>.getNotNull(key: String): T {
    val field = checkNotNull(get(key)) { "\"$key\" cannot be null" }
    return field as T
}

inline fun <reified T> Map<String, Any>.getOrNull(key: String): T? {
    val field = get(key) ?: return null
    return field as T
}
