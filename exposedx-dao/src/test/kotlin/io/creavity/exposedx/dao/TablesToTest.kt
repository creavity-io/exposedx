package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityManager
import io.creavity.exposedx.dao.entities.getValue
import io.creavity.exposedx.dao.entities.manyToOne
import io.creavity.exposedx.dao.entities.manyToOptional
import io.creavity.exposedx.dao.entities.nullable
import io.creavity.exposedx.dao.manager.asList
import io.creavity.exposedx.dao.manager.oneToMany


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

val RegionTable.schools by oneToMany(School.region, School)
val RegionTable.schoolsSecondary by oneToMany(School.secondaryRegion, School)
val CountryTable.regions by oneToMany(Region.country, Region)

val Region.schools by Region.schools.asList()
val Region.schoolsSecondary by Region.schoolsSecondary.asList()
val Country.regions by Country.regions.asList()

