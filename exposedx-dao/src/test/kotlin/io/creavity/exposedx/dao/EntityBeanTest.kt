package io.creavity.exposedx.dao

import org.assertj.core.api.Assertions.assertThat
import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.manager.new
import io.creavity.exposedx.dao.queryset.first
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityBeanTest {

    @Test
    fun `Create entity object with empty constructor`() {
        val region = Region()
        region.name = "Region"
        assertThat(region.name).isEqualTo("Region")
    }

    @Test
    fun `Create entity with reference`() {
        val region = Region()
        region.name = "Region"
        val school = School()
        school.region = region
        assertThat(school.region.name).isEqualTo("Region")
    }
}