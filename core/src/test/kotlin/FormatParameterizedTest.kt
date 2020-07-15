import com.juul.mcumgr.message.Format
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(value = Parameterized::class)
open class FormatParameterizedTest(val format: Format) {

    @Test
    fun `dummy test`() {
        // Avoid failure due to no test cases
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "format={0}")
        fun format(): Iterable<Format> {
            return listOf(
                Format.SMP,
                Format.OMP
            )
        }
    }
}
