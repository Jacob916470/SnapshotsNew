package com.example.snapshotsnew

import android.app.Application
import com.google.firebase.auth.FirebaseUser

class SnapshotsAplications: Application() {
    companion object {
        const val PATH_SNAPSHOTS = "snapshots"
        const val PROPERTY_LIKE_LIST = "likeList"

        lateinit var currentUser: FirebaseUser
    }
}