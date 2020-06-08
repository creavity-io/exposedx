# Exposed X 

Is a library over top of Exposed that implements a new DAO design, Exposed X is based on Django ORM.

# Features
- Handle entities without connection
- OneToMany, ManyToOne relations
- Lazy load
- Gson Serializer/Deserializer
- Implicit inner joins.

## Tables
For create tables, Exposed X use the same class for the manager and table as companion of Entity:

```kotlin
open class CountryTable: IntEntityTable<Country, CountryTable>() {
    val name by varchar("name", 255)
}
``` 
> The table should extends of EntityTable, and should be open
> Other thing to consider is that columns should be marked with "by" as delegator.

The bean have to extend of Entity class and should have the Manager as companion.
```kotlin
class Country: IntEntity() {
    companion object Table: CountryTable()
    var name by Table.name
}
```

We can reference the columns from `Country.name`, and create new instance from `Country()`
## One to Many

```kotlin
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
```

The one to many relations, are reverse relations of ManyToOne or Foreing keys, then it can be added from other module with extension getters. 

```kotlin
val CountryTable.regions by oneToMany(Region.country, Region)
val Country.regions by Country.regions.asList()
```

This will createt a queryset that we can iterate for example:

```
val country = Country.objects.firstt()
country.regions.forEach {
    print(it.name)
}
```

or build new queryset.

```
val country = Country.objects.firstt()
val regions = country.regions.filter { name eq "Lima" }
```

## Filters

Filters use ExpressionSqlBuilder of Exposed, this can be made from

```
Country.objects.filter { name like "A%" }
```

```
Region.objects.filter { country.name like "A%" }
```

## Joins

Joins are automatically made when you use a remote property, for example:

```
Region.objects.filter { country.name like "A%" }
```

You can use select related / prefetch related to query ManyToOne in  



## Cache
## Exposed DSL

## Custom Manager
## Custom Queryset

## Gson serializer

# How Works
## How works implicit inner join
When a filter/exclude is called, queryset create a copy of the table and save any field that is called in the filter for make a inner join.

## How works TableCache
TableCache is a transaction cache that:
- Keep only one object with the same id/table thought all transaction, if two objects references with the same id tries to be saved in cache It will raise a exception.
- Keep objects to save in a bulk sentence, when we will make a query of database or transaction is finished, all objects will be bulked to database.

# How to use 

# Roadmap
- Prefetch/Select Related in oneToMany queries.
- ManyToMany relations
- Cache of queryset
