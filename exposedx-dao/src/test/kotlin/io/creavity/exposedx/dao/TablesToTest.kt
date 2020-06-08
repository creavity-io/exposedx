package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityTable
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.entities.manyToOne
import io.creavity.exposedx.dao.entities.manyToOptional
import io.creavity.exposedx.dao.entities.nullable
import io.creavity.exposedx.dao.entities.asList
import io.creavity.exposedx.dao.entities.oneToManyRef


open class CountryTable: IntEntityTable<Country, CountryTable>() {
    val name by varchar("name", 255)
    val isActive by bool("is_active").default(false)

}

class Country: IntEntity() {
    companion object Table: CountryTable()
    var name by Table.name
    var isActive by Table.isActive

}


abstract class RegionTable: IntEntityTable<Region, RegionTable>() {
    val name by varchar("name", 255)
    val country by manyToOne("country_id", Country)
    val isActive by bool("is_active").default(false)

}

class Region: IntEntity() {
    companion object Table: RegionTable()
    var name by Table.name
    var country by Table.country
    var isActive by Table.isActive

}

open class SchoolTable: IntEntityTable<School, SchoolTable>() {
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

val Region.schools by Region.schools.asList()
val Region.schoolsSecondary by Region.schoolsSecondary.asList()
val Country.regions by Country.regions.asList()
val Country.region by Country.regions

