package io.creavity.exposedx.dao.exceptions

class MultipleEntityReturned()
    : Exception("The query returned multiple objects when only one was expected")