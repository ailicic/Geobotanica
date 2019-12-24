package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.SpekExt.mockTime
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


class GbTimeTest : Spek({
    mockTime()

    var now = "now"
    var later = "later"

    describe("Mock time") {
        beforeEachTest {
            now = GbTime.now().toString()
            Thread.sleep(2)
            later = GbTime.now().toString()
        }

        it("Should be constant time") { now shouldEqual later }
    }
})