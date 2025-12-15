package request

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.dto.request.ShareRequest

class ShareRequestTest {
    @Test
    fun `ShareRequest should have all properties`() {
        // When
        val request =
            ShareRequest(
                fromEmail = "from@example.com",
                toEmail = "to@example.com",
                role = "Guest",
            )

        // Then
        request.fromEmail shouldBeEqualTo "from@example.com"
        request.toEmail shouldBeEqualTo "to@example.com"
        request.role shouldBeEqualTo "Guest"
    }
}
