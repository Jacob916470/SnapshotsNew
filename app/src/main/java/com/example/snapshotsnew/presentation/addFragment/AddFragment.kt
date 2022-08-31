package com.example.snapshotsnew.presentation.addFragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import com.example.snapshotsnew.R
import com.example.snapshotsnew.SnapshotsAplications
import com.example.snapshotsnew.data.Snapshot
import com.example.snapshotsnew.databinding.FragmentAddBinding
import com.example.snapshotsnew.presentation.callBack.MainAux
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddFragment : Fragment() {

    //private val RC_GALLERY = 18

    /**Storag Firebase
     * Esto nos servira para que podamos crear una carpeta dentro de nustro servidor
     * y ahi podamos administrar todas nuestras imagenes de acuerdo al usuario */
    //private val PATH_SNAPSHOT = "snapshots"

    private lateinit var mStorageReference: StorageReference

    /** Se crea debido a que no es suficiente con subir la imagen a storage también tenemos que extraer la
     * "URL" y ponerla en "RealtimeDatabase" */
    private lateinit var mDatabaseReference: DatabaseReference


    private lateinit var mBinding: FragmentAddBinding

    private var mPhotoSelectUri: Uri? = null

    private var mainAux: MainAux? = null

    /** Actualización "onActivityResult" */
    private val galleryResutl =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                with(mBinding) {
                    mPhotoSelectUri = it.data?.data
                    imgPhoto.setImageURI(mPhotoSelectUri)
                    imgPhoto.setImageURI(mPhotoSelectUri)
                    tilTitle.visibility = View.VISIBLE
                    txtMessage.text = getString(R.string.post_valid_title)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentAddBinding.inflate(
            inflater,
            container,
            false
        )
        // Inflate the layout for this fragment
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTextField()
        setupButons()
        setupFirebase()

    }

    private fun setupTextField() {
        with(mBinding) {
            etTitle.addTextChangedListener { validateFields(tilTitle) }
        }
    }

    private fun setupButons() {
        with(mBinding) {
            btnPost.setOnClickListener { if (validateFields(tilTitle)) postSnapshot() }
            btnSelect.setOnClickListener { openGallery() }
        }
    }

    private fun setupFirebase() {
        /** Inicializamos mStorageReference */
        //mStorageReference = FirebaseStorage.getInstance().reference
        mStorageReference =
            FirebaseStorage.getInstance().reference.child(SnapshotsAplications.PATH_SNAPSHOTS)
        /** Inicializamos mDatabaseReference */
        //mDatabaseReference = FirebaseDatabase.getInstance().reference.child(PATH_SNAPSHOT)
        mDatabaseReference =
            FirebaseDatabase.getInstance().reference.child(SnapshotsAplications.PATH_SNAPSHOTS)
    }

    /** Con esto le decimos a la aplicación que seleccione una imagen y pueda regresar esa información */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //startActivityForResult(intent, RC_GALLERY)
        galleryResutl.launch(intent)
    }

    private fun postSnapshot() {
        with(mBinding) {
            if (mPhotoSelectUri != null) {
                enableUI(false)
                /** Se crea para ver el proceso cuado subimos nuestra imagen */
                mBinding.progressBar.visibility = View.VISIBLE

                val key = mDatabaseReference.push().key!!

                /** Creamos segunda referencia local ya que no se pudo insertar desde la referencia global */
                /** Cambiamos my_photo, para asi poder agregar imagen por usuario y se ocupara la key, ya que es unico para cada imagen */
                val storageReference = mStorageReference.child(SnapshotsAplications.currentUser.uid)
                    .child(key)
                //.child(FirebaseAuth.getInstance().currentUser!!.uid).child(key)
                /** Hacemos esto para poder hacer que diferentes usuarios puedan agregar fotos con su propio "uid"
                 * Se agregará ".child(key)" para que se identifique el usuario y asi podamos subir diferentes imagenes de acuerdo al usuario*/


                storageReference.putFile(mPhotoSelectUri!!)
                    /** Se crea para ir pintando ese progressBar para visualizar como se va subiendo
                     * la imagen */
                    .addOnProgressListener {
                        /** Es decir que vamos a calcular el porcentage de los bytes transferidos con
                         * respecto del total */
                        val progress = (100 * it.bytesTransferred / it.totalByteCount).toDouble()
                        progressBar.progress = progress.toInt()
                        //txtMessage.text = "$progress%"
                        txtMessage.text = String.format("%s%%", progress)
                    }
                    .addOnCompleteListener {
                        progressBar.visibility = View.INVISIBLE
                    }
                    .addOnSuccessListener { it ->
                        /** Sirve para que una vez que extraigamos la url con exito, procederemos a guardar
                         * esa foto */
                        it.storage.downloadUrl.addOnSuccessListener {
                            saveSnapshot(key, it.toString(), etTitle.text.toString().trim())
                        }
                            .addOnFailureListener {
                                Snackbar.make(
                                    root,
                                    R.string.post_message_image_fail,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                    }
            }
        }
    }

    /** Con este metodo podemos hacer una inserción dentro de "RealtimeDatabase"*/
    private fun saveSnapshot(key: String, url: String, title: String) {
        val snapshot = Snapshot(title = title, photoUrl = url)
        mDatabaseReference.child(key).setValue(snapshot)
            .addOnSuccessListener {
                hideKeyBoard()
                Snackbar.make(
                    mBinding.root,
                    getString(R.string.message_success),
                    Snackbar.LENGTH_LONG
                )
                    .show()

                /** Hace que nuestra imagen y cuadro de texto desaparezcan y se vuelva en blanco para poder añadir una nueva imagen */
                with(mBinding) {
                    tilTitle.visibility = View.GONE
                    etTitle.setText("")
                    tilTitle.error = null
                    txtMessage.text = getString(R.string.post_message_title)
                    imgPhoto.setImageDrawable(null)
                }

            }
            .addOnCompleteListener { enableUI(true) }
            .addOnFailureListener { (mainAux?.showMessage(R.string.post_message_image_fail)) }
    }

    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true

        for (textField in textFields) {
            if (textField.editText?.text.toString().trim().isEmpty()) {
                textField.error = getString(R.string.helper_requiered)
                isValid = false
            } else textField.error = null
        }

        return isValid

    }

    /** Escondemos del teclado */
    private fun hideKeyBoard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    /* Actualización
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_GALLERY) {
                with(mBinding) {
                    mPhotoSelectUri = data?.data
                    imgPhoto.setImageURI(mPhotoSelectUri)
                    imgPhoto.setImageURI(mPhotoSelectUri)
                    tilTitle.visibility = View.VISIBLE
                    txtMessage.text = getString(R.string.post_valid_title)
                }
            }

        }
    }
*/
    private fun enableUI(enable: Boolean) {
        with(mBinding) {
            btnSelect.isEnabled = enable
            btnPost.isEnabled = enable
            tilTitle.isEnabled = enable
        }
    }

}