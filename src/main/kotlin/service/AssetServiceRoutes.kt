package service

interface AssetServiceRoutes {
    fun get(
        directory: String,
        id: Long,
    ): String

    fun put(
        directory: String,
        id: Long,
        content: String,
    ): String

    fun delete(
        directory: String,
        id: Long,
    )

    fun exists(
        directory: String,
        id: Long,
    ): Boolean
}
