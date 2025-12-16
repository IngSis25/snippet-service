package snippets.dto.response

import snippets.model.Test

class TestDTO(test: Test) {
    val id: Long = test.id
    val name: String = test.name
    val input: List<String> = test.input
    val output: List<String> = test.output
}
