package codetrail.app

/** Thin JS bindings. Kept here so the rest of the web target reads like ordinary Kotlin. */
internal object Browser {
    fun get(key: String): String? = lsGet(key)
    fun set(key: String, value: String) = lsSet(key, value)
    fun remove(key: String) = lsRemove(key)
    fun keys(): List<String> = List(lsLength()) { lsKey(it) ?: "" }.filter { it.isNotEmpty() }
    fun reload() = jsReload()
    fun language(): String = jsLanguage()
    fun now(): Long = jsNow().toLong()
}

private fun lsGet(key: String): String? = js("localStorage.getItem(key)")
private fun lsSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")
private fun lsRemove(key: String): Unit = js("localStorage.removeItem(key)")
private fun lsLength(): Int = js("localStorage.length")
private fun lsKey(i: Int): String? = js("localStorage.key(i)")
private fun jsReload(): Unit = js("location.reload()")
private fun jsLanguage(): String = js("(navigator.language || 'en')")
private fun jsNow(): Double = js("Date.now()")
