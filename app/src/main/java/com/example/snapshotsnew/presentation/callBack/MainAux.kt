package com.example.snapshotsnew.presentation.callBack

import com.google.android.material.snackbar.Snackbar

/****
 * Project: Snapshots
 * From: com.cursosant.android.snapshots
 * Created by Alain Nicolás Tello on 12/26/20 at 7:40 PM
 * Course: Android Practical with Kotlin from zero.
 * All rights reserved 2021.
 *
 * All my Courses(Only on Udemy):
 * https://www.udemy.com/user/alain-nicolas-tello/
 ***/
interface MainAux {
    fun showMessage(resId: Int, duration: Int = Snackbar.LENGTH_SHORT)
}