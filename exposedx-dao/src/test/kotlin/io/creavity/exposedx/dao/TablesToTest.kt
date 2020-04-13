package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.manager.EntityManager
import io.creavity.exposedx.dao.manager.oneToMany
import io.creavity.exposedx.dao.manager.oneToManyRef
import io.creavity.exposedx.dao.queryset.EntityQuery
import io.creavity.exposedx.dao.queryset.EntityQueryBase
import io.creavity.exposedx.dao.queryset.EntitySizedIterable
import org.jetbrains.exposed.sql.Query
import kotlin.reflect.KProperty


open class CountryTable: IntEntityManager<Country, CountryTable>() {
    val name by varchar("name", 255)
}

class Country: IntEntity() {
    companion object Table: CountryTable()
    var name by Table.name
}


abstract class RegionTable: IntEntityManager<Region, RegionTable>() {
    val name by varchar("name", 255)
    val country by manyToOne("country", Country)
}

class Region: IntEntity() {
    companion object Table: RegionTable()
    var name by Table.name
    var country by Table.country
}

open class SchoolTable: IntEntityManager<School, SchoolTable>() {
    val name by varchar("name", 255)
    val region by manyToOne("region_id", Region)
    val secondaryRegion by manyToOptional("secondary_region_id", Region)
}



class School : IntEntity() {
    companion object Table: SchoolTable()

    var name by Table.name
    var region by Table.region
    var secondaryRegion by Table.secondaryRegion.nullable()
}

val RegionTable.schools by oneToManyRef(School.region, School)
val RegionTable.schoolsSecondary by oneToManyRef(School.secondaryRegion, School)
val CountryTable.regions by oneToManyRef(Region.country, Region)

val Region.schools by Region.schools.oneToMany()
val Region.schoolsSecondary by Region.schoolsSecondary.oneToMany()
val Country.regions by Country.regions.oneToMany()

