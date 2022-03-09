package com.hwx.chatique.helpers

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface IPreferencesStore {

    fun setFlagEnabled(key: String, value: Boolean)
    fun isFlagEnabled(key: String, default: Boolean = false): Boolean

    fun store(key: String, value: Any?)
    fun <T : Any?> restore(key: String, clazz: Class<T>): T?

    fun <T : Any?> storeList(key: String, value: List<T>)
    fun <T> restoreList(key: String, clazz: Class<T>): List<T>?

    fun storeInt(key: String, value: Int)
    fun restoreInt(key: String, default: Int = -1): Int

    fun storeLong(key: String, value: Long)
    fun restoreLong(key: String, default: Long = 0L): Long

    fun storeString(key: String, value: String?)
    fun restoreString(key: String, default: String? = null): String?

    fun removeByKey(key: String)
    fun containsKey(key: String): Boolean
}

class PreferencesStoreImpl(
    private val preferences: SharedPreferences,
    private val gson: Gson
) : IPreferencesStore {

    override fun setFlagEnabled(key: String, value: Boolean) =
        preferences.edit().putBoolean(key, value).apply()

    override fun isFlagEnabled(key: String, default: Boolean): Boolean =
        preferences.getBoolean(key, default)

    override fun store(key: String, value: Any?) =
        preferences.edit().putString(key, gson.toJson(value)).apply()

    override fun <T : Any?> restore(key: String, clazz: Class<T>): T? =
        gson.fromJson<T>(preferences.getString(key, ""), clazz)

    override fun <T> storeList(key: String, value: List<T>) =
        preferences.edit().putString(key, gson.toJson(value)).apply()

    override fun <T> restoreList(key: String, clazz: Class<T>): ArrayList<T>? =
        gson.fromJson<ArrayList<T>>(
            preferences.getString(key, ""),
            TypeToken.getParameterized(List::class.java, clazz).type
        )

    override fun storeLong(key: String, value: Long) =
        preferences.edit().putLong(key, value).apply()

    override fun restoreLong(key: String, default: Long): Long = preferences.getLong(key, default)

    override fun storeInt(key: String, value: Int) =
        preferences.edit().putInt(key, value).apply()

    override fun restoreInt(key: String, default: Int): Int = preferences.getInt(key, default)

    override fun storeString(key: String, value: String?) =
        preferences.edit().putString(key, value).apply()

    override fun restoreString(key: String, default: String?): String? =
        preferences.getString(key, default)

    override fun removeByKey(key: String) =
        preferences.edit().remove(key).apply()

    override fun containsKey(key: String): Boolean = preferences.contains(key)
}

class StorageDelegate<T>(
    private val getter: (String, T) -> T,
    private val setter: (String, T) -> Unit,
    private val default: T,
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
        getter(property.name, default)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
        setter(property.name, value)
}

class StorageObjectDelegate<T>(
    private val getter: (String, Class<T>) -> T?,
    private val setter: (String, T?) -> Unit,
    private val default: T?,
    private val targetClass: Class<T>,
) : ReadWriteProperty<Any, T?> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T? =
        getter(property.name, targetClass) ?: default

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) =
        setter(property.name, value)
}

fun IPreferencesStore.flag(default: Boolean = false) =
    StorageDelegate(::isFlagEnabled, ::setFlagEnabled, default)

fun IPreferencesStore.int(default: Int = 0) =
    StorageDelegate(::restoreInt, ::storeInt, default)

fun IPreferencesStore.long(default: Long = 0L) =
    StorageDelegate(::restoreLong, ::storeLong, default)

fun IPreferencesStore.string(default: String? = null) =
    StorageDelegate(::restoreString, ::storeString, default)

inline fun <reified T> IPreferencesStore.obj(default: T? = null) =
    StorageObjectDelegate(::restore, ::store, default, T::class.java)
