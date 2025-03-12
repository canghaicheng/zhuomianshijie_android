data class SttSetting(
    val name: String = "",
    val sttDomain: String = "",
    val sttPath: String = "",
    val sttKey: String = "",
    val sttModel: String = ""
) {
    override fun toString(): String = name
}
