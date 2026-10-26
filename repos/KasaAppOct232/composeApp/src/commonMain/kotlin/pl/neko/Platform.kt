package pl.neko

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform