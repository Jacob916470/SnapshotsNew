package com.example.snapshotsnew.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/** Se agrega anotación ya que es recomendable como nos lo indica firebase */
@IgnoreExtraProperties
/** Se agreda la sentencia para poder excluir el id a "Firebase"
 * Y para poder asignar el id correcpondiente, el cual jalaremos desde "Firebase" nos iremos al nuestro "HomeFragment" */
data class Snapshot(@get:Exclude
    var id: String = "",
    var title: String = "",
    var photoUrl: String = "",
    var likeList: Map<String,Boolean> = mutableMapOf()
)
