package com.example.clonecontacts

import java.io.Serializable

class Group(var nameGroup: String = "", val dsUser: MutableList<User> = mutableListOf()) :
    Serializable {
}