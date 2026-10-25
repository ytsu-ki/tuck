package com.example.starbox.system

import org.junit.Assert.assertEquals
import org.junit.Test

class OemBatteryTest {
    @Test
    fun detectsBrandsFromManufacturerOrBrand() {
        assertEquals(Oem.Xiaomi, OemBattery.detect("Xiaomi", "Redmi"))
        assertEquals(Oem.Xiaomi, OemBattery.detect("unknown", "POCO"))
        assertEquals(Oem.Huawei, OemBattery.detect("HUAWEI", "HUAWEI"))
        assertEquals(Oem.Honor, OemBattery.detect("HONOR", "HONOR"))
        assertEquals(Oem.Oppo, OemBattery.detect("realme", "realme"))
        assertEquals(Oem.OnePlus, OemBattery.detect("OnePlus", "OnePlus"))
        assertEquals(Oem.Vivo, OemBattery.detect("vivo", "iQOO"))
        assertEquals(Oem.Samsung, OemBattery.detect("samsung", "samsung"))
        assertEquals(Oem.Other, OemBattery.detect("Google", "google"))
        assertEquals(Oem.Other, OemBattery.detect(null, null))
    }
}
