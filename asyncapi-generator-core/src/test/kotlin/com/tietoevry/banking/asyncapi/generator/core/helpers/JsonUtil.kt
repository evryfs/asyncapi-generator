package com.tietoevry.banking.asyncapi.generator.core.helpers

import com.google.gson.GsonBuilder

object JsonUtil {
    private val gson = GsonBuilder()
//        .serializeNulls()
        .setPrettyPrinting()
        .create()

    fun toJson(obj: Any?): String = gson.toJson(obj)
}
