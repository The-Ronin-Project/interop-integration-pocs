package demo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

class DemoApplicationTests {

	@Test
	fun contextLoads() {
		val x = "1"
		println(x)

		val y: String? = null
		println(y)
		println(y?.length)

		val z: String? = "Hello"
		println(z)
		println(z?.length)
	}

}
