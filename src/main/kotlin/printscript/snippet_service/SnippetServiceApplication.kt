package printscript.snippet_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SnippetServiceApplication

fun main(args: Array<String>) {
	runApplication<SnippetServiceApplication>(*args)
}
