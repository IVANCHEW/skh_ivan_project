package com.example.skh_ivan_app

class UserManagement constructor(val _userName: String) {
    companion object UserDetails {

        val params = HashMap<String,String>()

        fun setUserName(_userName: String) {
            params["username"] = _userName
        }
    }

}