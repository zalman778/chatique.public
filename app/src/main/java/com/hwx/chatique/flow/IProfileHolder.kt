package com.hwx.chatique.flow

import com.hwx.chatique.helpers.IPreferencesStore
import com.hwx.chatique.helpers.obj

interface IProfileHolder {
    val userId: Long
    val username: String

    fun updateProfile(new: StoredProfile?)
    fun getProfile(): StoredProfile?

    data class StoredProfile(
        val id: Long = -1L,
        val username: String = "",
        val token: String = "",
        val encodedPin: String = "",
        val clearTextPin: String = "",
    )
}

class ProfileHolder(
    prefs: IPreferencesStore,
) : IProfileHolder {

    private var storedProfile: IProfileHolder.StoredProfile? by prefs.obj()

    private var cachedProfile = storedProfile

    override val userId = cachedProfile?.id ?: -1L

    override val username = cachedProfile?.username ?: ""

    override fun getProfile() = storedProfile

    override fun updateProfile(new: IProfileHolder.StoredProfile?) {
        storedProfile = new
        cachedProfile = new
    }

}