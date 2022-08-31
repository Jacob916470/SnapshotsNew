package com.example.snapshotsnew.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.snapshotsnew.presentation.callBack.FragmentAux
import com.example.snapshotsnew.R
import com.example.snapshotsnew.SnapshotsAplications
import com.example.snapshotsnew.databinding.ActivityMainBinding
import com.example.snapshotsnew.presentation.addFragment.AddFragment
import com.example.snapshotsnew.presentation.homeFragment.HomeFragment
import com.example.snapshotsnew.presentation.profileFragment.ProfileFragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    /** Request code */
    //private val RC_SIGN_IN = 21

    private lateinit var mBinding: ActivityMainBinding

    /** Variable para identificar cual fragmento esta activo en la vista */
    private lateinit var mActiveFragment: Fragment

    /** Variable para la creación de nuestros fragmentos */
    private var mFragmentManager: FragmentManager? = null

    /** Autenticación de usuarios */
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private var mFirebaseAuth: FirebaseAuth? = null

    private val authResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienbenido...", Toast.LENGTH_SHORT).show()
            } else {
                /** Esto significa que el usuario rechazo nuestra autentificación "UI"*/
                if (IdpResponse.fromResultIntent(it.data) == null) {
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupAuth()
    }

    /** Se crea método para poder autentificar al uduario */
    private fun setupAuth() {
        /** Igualamos nuestra variable a "FirebaseAuth.getInstance()" para obtener una instancia */
        mFirebaseAuth = FirebaseAuth.getInstance()
        /** Igualamos nuestra variable a un listener para asi poder ingresar mediante autenticación */
        mAuthListener = FirebaseAuth.AuthStateListener {
            //val user = it.currentUser
            if (it.currentUser == null) {
                /* Actualización
                 startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                  /** Nos deja de mostrar las cuentas logeadas */
                     .setIsSmartLockEnabled(false)
                         .setAvailableProviders(
                             Arrays.asList(
                                 AuthUI.IdpConfig.EmailBuilder().build(),
                                 AuthUI.IdpConfig.GoogleBuilder().build()
                             )
                         )
                         /** Solicita un "Request Code" es por eso que se crea uno */
                         .build(), SnapshotsAplications.RC_SIGN_IN*/
                authResult.launch(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        /** Nos deja de mostrar las cuentas logeadas */
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(
                            listOf(
                                AuthUI.IdpConfig.EmailBuilder().build(),
                                AuthUI.IdpConfig.GoogleBuilder().build()
                            )
                        )
                        /** Solicita un "Request Code" es por eso que se crea uno */
                        .build()
                )
            } else {
                SnapshotsAplications.currentUser = it.currentUser!!

                val fragmentProfile =
                    mFragmentManager?.findFragmentByTag(ProfileFragment::class.java.name)
                fragmentProfile?.let { fragment ->
                    (fragment as FragmentAux).refresh()
                }

                if (mFragmentManager == null) {
                    mFragmentManager = supportFragmentManager
                    setupBottonNav(mFragmentManager!!)
                }
            }
        }
    }

    private fun setupBottonNav(fragmentManager: FragmentManager) {
        mFragmentManager?.let { // TODO: 14/06/21 clean before
            for (fragment in it.fragments) {
                it.beginTransaction().remove(fragment!!).commit()
            }
        }

        /** Creamos nuestras variables con los nombres de nuestros fragmentos para asi identificarlas
         * mas facil y al mismo tiempo las instaciaremos con sus correcpondientes fragmentos */
        val homeFragment = HomeFragment()
        val addFragment = AddFragment()
        val profileFragment = ProfileFragment()

        /** Pondremos en nuestra variable "mActivefragment" el fragmento que queremos que este activo en un
         * inicio */
        mActiveFragment = homeFragment
        /** Empezaremos creando nustras vistas de nuestros fragmentos con las siguientes propiedades.
         * Tambien a cada uno de ellos iremos creandolo y ocultandolo, excepto el ultime que queremos que se vea
         * desde un inicio, tambien empezaremos programando desde el ultimo hasta el primero */
        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, profileFragment, ProfileFragment::class.java.name)
            .hide(profileFragment).commit()
        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, addFragment, AddFragment::class.java.name)
            .hide(addFragment).commit()
        fragmentManager.beginTransaction()
            .add(R.id.hostFragment, homeFragment, HomeFragment::class.java.name).commit()

        mBinding.bottonNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.action_home -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(homeFragment)
                        .commit()
                    mActiveFragment = homeFragment
                    true
                }
                R.id.action_add -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(addFragment)
                        .commit()
                    mActiveFragment = addFragment
                    true
                }
                R.id.action_profile -> {
                    fragmentManager.beginTransaction().hide(mActiveFragment).show(profileFragment)
                        .commit()
                    mActiveFragment = profileFragment
                    true
                }
                else -> false
            }
        }
        /** Se utilizará para poder decirle al activity quw se dirigira hasta arriba del fragmento */
        mBinding.bottonNav.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.action_home -> (homeFragment as FragmentAux).refresh()
            }
        }
    }

    /** Se añade ese listener con el ciclo de vida */
    override fun onResume() {
        super.onResume()
        /** Preguntamos si no es nulo y le pasamos nuesta variable "mAuthListener" */
        mFirebaseAuth?.addAuthStateListener(mAuthListener)
    }

    /** Se añade para poder liberar recursos */
    override fun onPause() {
        super.onPause()
        mFirebaseAuth?.removeAuthStateListener(mAuthListener)
    }

    /* Actualización
    /** Trataremos el caso de nuestro "FirebaseUI"
     * Y se preguntara de forma inversa, es decir primero preguntaremos si el "Requestcode es de nosotros "*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /** Una vez después de preguntar si es nuestro en case de que se trate de nuestro intent */
        if (requestCode == RC_SIGN_IN) {
            /** Preguntaremos si salio bien o no bien la autenticación,si inicio bien significa que pudo autenticarse bien
             * y automaticamente regreso a esa pantalla */
            if (requestCode == RESULT_OK) {
                Toast.makeText(this, "Bienbenido...", Toast.LENGTH_SHORT).show()
            } else {
                /** Esto significa que el usuario rechazo nuestra autentificación "UI"*/
                if (IdpResponse.fromResultIntent(data) == null) {
                    finish()
                }
            }

        }
    }*/
}