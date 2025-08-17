package com.example.clonecontacts.Model

import java.io.Serializable

class Group(var nameGroup: String = "", val dsUser: MutableList<User> = mutableListOf()) :
    Serializable {
}