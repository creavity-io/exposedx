package io.creavity.exposedx.dao

import io.creavity.exposedx.dao.entities.*
import io.creavity.exposedx.dao.entities.generics.IntEntity
import io.creavity.exposedx.dao.entities.generics.IntEntityTable


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


open class CompanyTable: IntEntityTable<Company, CompanyTable>() {
    val name by varchar("name", 255)
    val manager by oneToOne("manager_id", Manager)
    val contactInfo by oneToOptional("contact_info_id", ContactInfo)
}

class Company : IntEntity() {
    companion object Table: CompanyTable()

    var name by Table.name
    var manager by Table.manager
    var contactInfo by Table.contactInfo.nullable()

}

open class ContactInfoTable: IntEntityTable<ContactInfo, ContactInfoTable>() {
    val phone by varchar("phone", 255)
    val address by varchar("address", 255)
    val country by manyToOne("country", Country)
}

class ContactInfo : IntEntity() {
    companion object Table: ContactInfoTable()
    var phone by Table.phone
    var address by Table.address
    var country by Table.country
}

open class ManagerTable: IntEntityTable<Manager, ManagerTable>() {
    val name by varchar("name", 255)
    val country by manyToOne("country", Country)
}

class Manager : IntEntity() {
    companion object Table: ManagerTable()
    var name by Table.name
    var country by Table.country
}



val RegionTable.schools by oneToManyRef(School.region, School)
val RegionTable.schoolsSecondary by oneToManyRef(School.secondaryRegion, School)
val CountryTable.regions by oneToManyRef(Region.country, Region)

val Region.schools by Region.schools.asList()
val Region.schoolsSecondary by Region.schoolsSecondary.asList()
val Country.regions by Country.regions.asList()


val ManagerTable.company by oneToOneRef(Company.manager, Company)
val ContactInfoTable.company by oneToOneRef(Company.contactInfo, Company)
val CountryTable.managers by oneToManyRef(Manager.country, Manager)
val CountryTable.contactInfos by oneToManyRef(ContactInfo.country, ContactInfo)

val Manager.company by Manager.company
val ContactInfo.company by ContactInfo.company.nullable()
val Country.managers by Country.managers.asList()
val Country.contactInfos by Country.contactInfos.asList()
