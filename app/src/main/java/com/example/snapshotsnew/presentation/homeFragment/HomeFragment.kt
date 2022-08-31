package com.example.snapshotsnew.presentation.homeFragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.snapshotsnew.presentation.callBack.FragmentAux
import com.example.snapshotsnew.R
import com.example.snapshotsnew.SnapshotsAplications
import com.example.snapshotsnew.data.Snapshot
import com.example.snapshotsnew.databinding.FragmentHomeBinding
import com.example.snapshotsnew.databinding.ItemSnapshotBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class HomeFragment : Fragment(), FragmentAux {

    private lateinit var mBinding: FragmentHomeBinding

    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Snapshot, SnapshotHolder>
    private lateinit var mLayoutMainBinding: RecyclerView.LayoutManager

    /** Instanciamos nuestra variable "mSnapshotRef" como "DatabaseRefence" para asi poder diminuir código */
    private lateinit var mSnapshotsRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFirebase()
        setupAdapter()
        setupRecyclerView()

    }

    private fun setupFirebase() {
        mSnapshotsRef =
            FirebaseDatabase.getInstance().reference.child(SnapshotsAplications.PATH_SNAPSHOTS)
    }

    private fun setupAdapter() {
        /** Creamos esto debido a que tenemos la dependencia de "firebaseUi" */

        /** Podemos escribir la ruta donde sera almacenada la información, estas rutas tambien se llaman nodos
         * Es la rama que configuramos en firebase "snapshots"
         * reference = raíz
         * child = que rama, es decir "snapshots" */
        //val query = FirebaseDatabase.getInstance().reference.child("snapshots")
        val query = mSnapshotsRef

        /** De esta forma asignamos que nuestro id sera igual a nuestra rama que tenemos en "Firebase" */
        val options = FirebaseRecyclerOptions.Builder<Snapshot>().setQuery(query) {
            val snapshot = it.getValue(Snapshot::class.java)
            snapshot!!.id = it.key!!
            snapshot
        }.build()
        //.setQuery(query, Snapshot::class.java).build()

        /** De esta forma creamos nuestro RecyclerView de una forma sintetisada sin tener que obtener el "itemaccount" */
        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Snapshot, SnapshotHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotHolder {
                mContext = parent.context
                val view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_snapshot, parent, false)
                return SnapshotHolder(view)
            }

            override fun onBindViewHolder(holder: SnapshotHolder, position: Int, model: Snapshot) {
                val snapshot = getItem(position)

                with(holder) {
                    setListener(snapshot)
                    with(binding) {
                        txtTitle.text = snapshot.title
                        /** CheckedBox para verificar cuantos likes ha tenido la imagen */
                        cbLike.text = snapshot.likeList.keys.size.toString()
                        /** verificamos si hay un usuario */
                        FirebaseAuth.getInstance().currentUser?.let {
                            cbLike.isChecked = snapshot.likeList
                                .containsKey(it.uid)
                        }
                        /** Image */
                        Glide
                            .with(mContext)
                            .load(snapshot.photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(imgPhoto)
                    }
                }
            }
            /** Sobrecribimos este método para asi poder poner un stop a nuestro "progressBar" */
            /** Error interno firebase ui 8.0.0, esto debido a que si caramos una imagen despues de que cargue nuestra
             * información nos marca un error en el adapter */
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChanged() {
                super.onDataChanged()
                mBinding.progessBar.visibility = View.GONE
                /** Notificamos que habrá un cambio, asi podra cargar la data en nuestro "FragmentHome" y asi podremos sin
                 * ningún porblema cargar una  imagen desde nuestra galeria */
                notifyDataSetChanged()
            }

            /** Sobrescribimos este método para mostrara un error */
            override fun onError(error: DatabaseError) {
                super.onError(error)
                //Toast.makeText(mContext, error.message, Toast.LENGTH_SHORT).show()
                Snackbar.make(mBinding.root, error.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        /** Configuramos nuestro "RecyclerView" */
        mLayoutMainBinding = LinearLayoutManager(context)

        mBinding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutMainBinding
            adapter = mFirebaseAdapter
        }
    }

    /** Para poder inicializar nuestra aplicación con realtime Database necesitamos crear la rama (Base de datos)*/

    /** Le indicamos cuando va a comenzar a consumir los datos */
    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    /** Le indicamos cuando va a parar de consumir los datos */
    override fun onStop() {
        super.onStop()
        mFirebaseAdapter.stopListening()
    }

    override fun refresh() {
        mBinding.recyclerView.smoothScrollToPosition(0)
    }

    /** Eventos de click */
    private fun deleteSnapshot(snapshot: Snapshot) {
        /** Confirmar delete */
        context?.let {
        MaterialAlertDialogBuilder(it)
            .setTitle(R.string.dialog_delete_title)
            .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                /** Borramos la imagen de storage */
                val storageSnapshotsRef = FirebaseStorage.getInstance().reference
                    .child(SnapshotsAplications.PATH_SNAPSHOTS)
                    .child(SnapshotsAplications.currentUser.uid)
                    .child(snapshot.id)
                storageSnapshotsRef.delete().addOnCompleteListener { result ->
                    /** Significa que no hubo ningun error y la fotografia fue eliminada correctamente, primero se
                     * eliminara la foto y luego el registeo si es que fue exitoso */
                    if (result.isSuccessful){
                        /** Crearemos una instancia en la cual le indicaremos cual le indicamos en nombre de nuestra colección de "Firebase" */
                        //val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots")
                        /** Eliminamos datos */
                        mSnapshotsRef.child(snapshot.id).removeValue()
                    }else {
                        Snackbar.make(mBinding.root,R.string.message_error_delete_photo,Snackbar.LENGTH_LONG).show()
                    }

                }
            }
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show()
    }
}

/** Creamos los likes que seran dados en la imagen */
private fun setLike(snapshot: Snapshot, checked: Boolean) {
    /** Inicializamos "databaseReference" en nuestra rama */
    //val databaseReference = FirebaseDatabase.getInstance().reference.child("snapshots")
    val myUserRef = mSnapshotsRef.child(snapshot.id)
            .child(SnapshotsAplications.PROPERTY_LIKE_LIST)
            .child(SnapshotsAplications.currentUser.uid)
    /** Preguntamos si esta seleccionado */
    if (checked) {
        /** Accedemos a una nueva rama "likeList" la cual es la que creamos en nuestra base de datos de android */
        //databaseReference.child(snapshot.id).child("likeList")
            //.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(checked)
        myUserRef.setValue(checked)
    } else {
        /** Utilizamos este codigo para quitarle nuestro like */
        //databaseReference.child(snapshot.id).child("likeList")
            //.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(null)
        myUserRef.setValue(null)
    }
}

inner class SnapshotHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = ItemSnapshotBinding.bind(view)

    fun setListener(snapshot: Snapshot) {
        with(binding) {
            btnDelete.setOnClickListener { deleteSnapshot(snapshot) }
            cbLike.setOnCheckedChangeListener { _, checked ->
                setLike(snapshot, checked)
            }
        }
    }
}
}


