data class ApiSetting(
    var name: String = "",
    var apiMode: String = "openai",
    var apiDomain: String = "",
    var apiPath: String = "",
    var apiKey: String = "",
    var model: String = "",
    var maxSize: Int = 32
) {
    override fun toString(): String = name
}