import com.juul.mcumgr.message.Protocol
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
open class ProtocolParameterizedTest(val protocol: Protocol) {

    @Test
    fun `dummy test`() {
        // Avoid failure due to no test cases
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "format={0}")
        fun format(): Iterable<Protocol> {
            return listOf(
                Protocol.SMP,
                Protocol.OMP
            )
        }
    }
}
