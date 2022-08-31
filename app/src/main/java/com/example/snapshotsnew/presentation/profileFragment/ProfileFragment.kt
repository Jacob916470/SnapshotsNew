package com.example.snapshotsnew.presentation.profileFragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.snapshotsnew.R
import com.example.snapshotsnew.SnapshotsAplications
import com.example.snapshotsnew.databinding.FragmentProfileBinding
import com.example.snapshotsnew.presentation.callBack.FragmentAux
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment(), FragmentAux {

    private lateinit var mBinding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentProfileBinding.inflate(
            inflater,
            container,
            false
        )
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh()
        setupButton()
    }

    private fun setupButton() {
        mBinding.btnLogout.setOnClickListener {
            context?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.dialog_logout)
                    .setPositiveButton(R.string.dialog_logout_confirm) { _, _ ->
                        signOut()
                    }
                    .setNegativeButton(R.string.dialog_cancel_sesion,null)
                    .show()
            }
        }

    }

    /** Cerramos sesión */
    private fun signOut() {
        context?.let {
            AuthUI.getInstance().signOut(it)
                .addOnCompleteListener {
                    Toast.makeText(context, getString(R.string.log_out), Toast.LENGTH_SHORT).show()
                    mBinding.txtName.text = ""
                    mBinding.txtMail.text = ""

                    (activity?.findViewById(R.id.bottonNav) as? BottomNavigationView)?.selectedItemId =
                        R.id.action_home
                }
        }
    }

    override fun refresh() {
        with(mBinding) {
            txtName.text = SnapshotsAplications.currentUser.displayName
            txtMail.text = SnapshotsAplications.currentUser.email

        }
    }
}