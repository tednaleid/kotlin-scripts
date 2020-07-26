var value: Int = 1

println("value in importme: $value")

data class Detective(val name: String = "unknown")

val sherlock = Detective(name = "Sherlock Holmes")
