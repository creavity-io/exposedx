package io.creavity.exposedx.adapters

import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityTable
import io.creavity.exposedx.dao.entities.generics.UUIDEntity
import io.creavity.exposedx.dao.entities.generics.UUIDEntityTable
import io.creavity.exposedx.dao.entities.asList
import io.creavity.exposedx.dao.entities.oneToMany


open class CountryTable: IntEntityTable<Country, CountryTable>() {
    val name by varchar("name", 255)
}

class Country: IntEntity() {
    companion object Table: CountryTable()
    var name by Table.name
}


abstract class RegionTable: IntEntityTable<Region, RegionTable>() {
    val name by varchar("name", 255)
    val country by manyToOne("country", Country)
}

class Region: IntEntity() {
    companion object Table: RegionTable()
    var name by Table.name
    var country by Table.country
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

val RegionTable.schools by oneToMany(School.region, School)
val RegionTable.schoolsSecondary by oneToMany(School.secondaryRegion, School)
val CountryTable.regions by oneToMany(Region.country, Region)

val Region.schools by Region.schools.asList()
val Region.schoolsSecondary by Region.schoolsSecondary.asList()
val Country.regions by Country.regions.asList()


open class CountryUUIDTable: UUIDEntityTable<CountryUUID, CountryUUIDTable>() {
    val name by varchar("name", 255)
}



class CountryUUID : UUIDEntity() {
    companion object Table: CountryUUIDTable()

    var name by Table.name
}
