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
    fun copy(text: String) = jsCopy(text)
    fun download(name: String, text: String) = jsDownload(name, text)
    fun pickTextFile(onLoaded: (String) -> Unit) = jsPickTextFile(onLoaded)
}

private fun lsGet(key: String): String? = js("localStorage.getItem(key)")
private fun lsSet(key: String, value: String): Unit = js("localStorage.setItem(key, value)")
private fun lsRemove(key: String): Unit = js("localStorage.removeItem(key)")
private fun lsLength(): Int = js("localStorage.length")
private fun lsKey(i: Int): String? = js("localStorage.key(i)")
private fun jsReload(): Unit = js("location.reload()")
private fun jsLanguage(): String = js("(navigator.language || 'en')")
private fun jsNow(): Double = js("Date.now()")
private fun jsCopy(text: String): Unit = js("""{
    const fallback = () => {
        const ta = document.createElement('textarea');
        ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select();
        try { document.execCommand('copy'); } catch (e) {}
        document.body.removeChild(ta);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(text).catch(fallback);
    else fallback();
}""")
private fun jsDownload(name: String, text: String): Unit = js("""{
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; document.body.appendChild(a); a.click();
    document.body.removeChild(a); URL.revokeObjectURL(url);
}""")
private fun jsPickTextFile(onLoaded: (String) -> Unit): Unit = js("""{
    const input = document.createElement('input');
    input.type = 'file'; input.accept = '.properties,.txt,text/plain';
    input.onchange = () => {
        const f = input.files && input.files[0];
        if (f) f.text().then(onLoaded);
    };
    input.click();
}""")
