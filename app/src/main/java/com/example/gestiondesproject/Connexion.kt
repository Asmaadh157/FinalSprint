package com.example.gestiondesproject

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.gestiondesproject.databinding.ConnexionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Connexion : AppCompatActivity() {
    private lateinit var binding: ConnexionBinding
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConnexionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Lien vers l'écran d'inscription
        binding.textViewSignUp.setOnClickListener {
            val signUpIntent = Intent(this, Inscrire::class.java)
            startActivity(signUpIntent)
        }

        // Gestion de la connexion
        binding.button1.setOnClickListener {
            val email = binding.editTextText.text.toString().trim()
            val pass = binding.editTextText5.text.toString().trim()

            if (validateInputs(email, pass)) {
                loginUser(email, pass)
            }
        }

    }

    // Fonction de validation des champs
    private fun validateInputs(email: String, pass: String): Boolean {
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Les champs email et mot de passe ne doivent pas être vides !", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "L'email entré n'est pas valide !", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // Fonction de connexion de l'utilisateur
    private fun loginUser(email: String, pass: String) {
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                if (user?.isEmailVerified == true) {
                    // Récupérer le rôle de l'utilisateur
                    fetchUserRoleAndRedirect(user.uid)
                } else {
                    Toast.makeText(this, "Veuillez vérifier votre email avant de vous connecter.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, task.exception?.message ?: "Erreur de connexion inconnue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fonction pour récupérer le rôle de l'utilisateur et rediriger
    private fun fetchUserRoleAndRedirect(uid: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        databaseRef.get().addOnSuccessListener { dataSnapshot ->
            val role = dataSnapshot.child("status").value.toString()

            when (role) {
                "Professeur" -> {
                    val status = dataSnapshot.child("status").value.toString()
                    intent.putExtra("status",status)

                    navigateToInterface(Interface2::class.java)

                }
                "Étudiant" -> {
                    // Retrieve group info for the student
                    val groupeId = dataSnapshot.child("groupId").value.toString()
                    val groupeNom = dataSnapshot.child("nomGroupe").value.toString()
                    val status = dataSnapshot.child("status").value.toString()

                    // Pass the group data to MenuGroupeActivity
                    val intent = Intent(this, MenuGroupeActivity::class.java)
                    intent.putExtra("groupeId", groupeId)
                    intent.putExtra("nomGroupe", groupeNom)
                    intent.putExtra("status",status)
                    startActivity(intent)
                    finish() // Close the current activity
                }
                else -> {
                    Toast.makeText(this, "Rôle inconnu. Contactez l'administrateur.", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erreur lors de la récupération des informations de l'utilisateur.", Toast.LENGTH_SHORT).show()
        }

    }

    // Fonction générique pour naviguer vers une interface spécifique
    private fun navigateToInterface(destination: Class<*>) {
        val intent = Intent(this, destination)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
    fun  togglePasswordVisibility(view: View) {
        val passwordEditText = findViewById<EditText>(R.id.editTextText5)
        val inputType = passwordEditText.inputType

        if (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Currently visible, hide it
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            (view as ImageButton).setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            // Currently hidden, show it
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            (view as ImageButton).setImageResource(R.drawable.baseline_remove_red_eye_24)
        }

        passwordEditText.setTypeface(ResourcesCompat.getFont(this, R.font.helvetica))
        // Move cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.length())
    }

}

