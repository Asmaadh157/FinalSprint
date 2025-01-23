package com.example.gestiondesproject

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.gestiondesproject.databinding.InscrireBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference

class Inscrire : AppCompatActivity() {
    private lateinit var binding: InscrireBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InscrireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")

        Log.d("Inscription", "Activité d'inscription démarrée.")

        setupSpinner()
        setupRegistrationButton()

        // Gestion du clic sur l'icône de retour
        val backButton: ImageButton = binding.root.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()  // Cela ferme l'activité et revient à l'activité précédente
        }
    }

    private fun setupSpinner() {
        val spinnerStatus = binding.spinnerStatus
        val editTextStatus = binding.editTextStatus
        editTextStatus.visibility = View.GONE

        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = parentView?.getItemAtPosition(position).toString()
                Log.d("Inscription", "Statut sélectionné : $selectedStatus")
                when (selectedStatus) {
                    "Professeur" -> {
                        editTextStatus.visibility = View.VISIBLE
                        editTextStatus.hint = "DOTI"
                    }
                    "Étudiant" -> {
                        editTextStatus.visibility = View.VISIBLE
                        editTextStatus.hint = "Appogee"
                    }
                    else -> editTextStatus.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                editTextStatus.visibility = View.GONE
            }
        }
    }

    private fun setupRegistrationButton() {
        binding.button2.setOnClickListener {
            val email = binding.editTextText3.text.toString().trim()
            val pass = binding.editTextText5.text.toString().trim()
            val confirmPass = binding.editTextText6.text.toString().trim()
            val status = binding.spinnerStatus.selectedItem.toString()
            val statusNumber = binding.editTextStatus.text.toString().trim()
            val name = binding.editTextText.text.toString().trim()
            val prenom = binding.editTextText2.text.toString().trim()

            if (validateInputs(email, pass, confirmPass, status, statusNumber, name, prenom)) {
                checkIfEmailExists(email) { isEmailExists ->
                    if (isEmailExists) {
                        Toast.makeText(this, "Cet email est déjà utilisé !", Toast.LENGTH_SHORT).show()
                    } else {
                        if (status == "Étudiant") {
                            checkIfStatusNumberExists(statusNumber, status) { isStatusNumberExists ->
                                if (isStatusNumberExists) {
                                    Toast.makeText(this, "L'Appogée est déjà utilisé !", Toast.LENGTH_SHORT).show()
                                } else {
                                    checkIfAppogeeInGroup(statusNumber) { isInGroup, groupId ->
                                        if (!isInGroup) {
                                            Toast.makeText(this, "L'Appogée n'est pas déclaré dans un groupe par un professeur.", Toast.LENGTH_SHORT).show()
                                        } else if (pass == confirmPass) {
                                            registerNewUser(email, pass, status, statusNumber, name, prenom, groupId)
                                        } else {
                                            Toast.makeText(this, "Les mots de passe ne correspondent pas !", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        } else if (status == "Professeur") {
                            checkIfStatusNumberExists(statusNumber, status) { isStatusNumberExists ->
                                if (isStatusNumberExists) {
                                    Toast.makeText(this, "Le DOTI est déjà utilisé !", Toast.LENGTH_SHORT).show()
                                } else if (pass == confirmPass) {
                                    registerNewUser(email, pass, status, statusNumber, name, prenom, null)
                                } else {
                                    Toast.makeText(this, "Les mots de passe ne correspondent pas !", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkIfEmailExists(email: String, callback: (Boolean) -> Unit) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    callback(!signInMethods.isNullOrEmpty())  // Si la liste n'est pas vide, l'email existe déjà
                } else {
                    Toast.makeText(this, "Erreur de vérification de l'email : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }
    }

    private fun checkIfStatusNumberExists(statusNumber: String, status: String, callback: (Boolean) -> Unit) {
        val query = userDatabase.orderByChild("statusNumber").equalTo(statusNumber)
        query.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                callback(snapshot.exists()) // Si un utilisateur existe avec ce statusNumber
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@Inscrire, "Erreur de vérification : ${error.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun checkIfAppogeeInGroup(appogee: String, callback: (Boolean, String?) -> Unit) {
        val query = FirebaseDatabase.getInstance().getReference("Groupes")
        query.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var exists = false
                var groupId: String? = null
                for (groupSnapshot in snapshot.children) {
                    val membres = groupSnapshot.child("membres").children
                    for (membre in membres) {
                        val membreAppogee = membre.child("appogee").value.toString()
                        if (membreAppogee == appogee) {
                            exists = true
                            groupId = groupSnapshot.key // récupère l'ID du groupe

                            break
                        }
                    }
                    if (exists) break
                }
                callback(exists, groupId)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@Inscrire, "Erreur de vérification des groupes : ${error.message}", Toast.LENGTH_SHORT).show()
                callback(false, null)
            }
        })
    }

    private fun validateInputs(email: String, pass: String, confirmPass: String, status: String, statusNumber: String, name: String, prenom: String): Boolean {
        if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty() || status == "Choisir un statut" || statusNumber.isEmpty() || name.isEmpty() || prenom.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires !", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Entrez un email valide", Toast.LENGTH_SHORT).show()
            return false
        }

        if (pass.length < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerNewUser(email: String, pass: String, status: String, statusNumber: String, name: String, prenom: String, groupId: String?) {
        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                saveUserToDatabase(userId, email, status, statusNumber, name, prenom, groupId)


            } else {
                Toast.makeText(this, "Erreur lors de l'inscription : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseAuth", "Erreur : ${task.exception?.message}")
            }
        }
    }

    private fun saveUserToDatabase(userId: String, email: String, status: String, statusNumber: String, name: String, prenom: String, groupId: String?) {
        val userInfo = mapOf(
            "email" to email,
            "status" to status,
            "statusNumber" to statusNumber,
            "name" to name,
            "prenom" to prenom,
            "groupId" to groupId // Enregistrer l'ID du groupe
        )
        userDatabase.child(userId).setValue(userInfo).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Pass the groupId to MenuEtudiant activity after registration
                sendEmailVerification()
                val intent = Intent(this, Connexion::class.java)

                intent.putExtra("groupeId", groupId)
                intent.putExtra("nomGroupe", "Group Name")
                intent.putExtra("status",status)// You can fetch the group name as needed
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Erreur Firebase : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseDatabase", "Erreur : ${task.exception?.message}")
            }
        }
    }

    private fun sendEmailVerification() {
        val user = firebaseAuth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Email de vérification envoyé. Vérifiez votre boîte de réception.", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            } else {
                Toast.makeText(this, "Erreur lors de l'envoi de l'email de vérification : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseAuth", "Erreur : ${task.exception?.message}")
            }
        }
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, Connexion::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(loginIntent)
    }
    fun  togglePasswordVisibility(view: View) {
        val passwordEditText: EditText = when (view.id) {
            R.id.togglePasswordVisibility1 -> findViewById(R.id.editTextText5)  // Password field
            R.id.togglePasswordVisibility2 -> findViewById(R.id.editTextText6)  // Confirm password field
            else -> return  // If the view id does not match, return
        }
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
        passwordEditText.setTypeface(ResourcesCompat.getFont(this, R.font.ariel_ce))


        // Move cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.length())
    }
}
