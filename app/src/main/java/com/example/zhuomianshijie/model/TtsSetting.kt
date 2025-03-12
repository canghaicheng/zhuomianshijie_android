data class TtsSetting(
    var name: String = "",
    var ttsDomain: String = "",
    var ttsPath: String = "",
    var ttsKey: String = "",
    var ttsModel: String = "",
    var ttsVoiceId: String = ""
) {
    override fun toString(): String = name
}