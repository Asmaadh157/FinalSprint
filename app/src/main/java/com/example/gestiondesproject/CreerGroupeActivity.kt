package com.example.gestiondesproject

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gestiondesproject.databinding.ActivityCreerGroupeBinding
import com.google.firebase.database.FirebaseDatabase

class CreerGroupeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreerGroupeBinding
    private val membres = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreerGroupeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRetour.setOnClickListener {
            finish()
        }

        binding.btnAjouterMembre.setOnClickListener {
            afficherAjouterMembreDialog()
        }

        binding.btnCreerGroupe.setOnClickListener {
            val nomGroupe = binding.etNomGroupe.text.toString().trim()
            val nomProjet = binding.etNomProjet.text.toString().trim()
            val dateDebut = binding.etDateDebut.text.toString().trim()
            val dateFin = binding.etDateFin.text.toString().trim()

            if (validerChamps(nomGroupe, nomProjet, dateDebut, dateFin)) {
                creerGroupe(nomGroupe, nomProjet, dateDebut, dateFin)
            }
        }
    }

    private fun validerChamps(nomGroupe: String, nomProjet: String, dateDebut: String, dateFin: String): Boolean {
        if (nomGroupe.isEmpty() || nomProjet.isEmpty() || dateDebut.isEmpty() || dateFin.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (membres.isEmpty()) {
            Toast.makeText(this, "Ajoutez au moins un membre au groupe.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun afficherAjouterMembreDialog() {
        val ajouterMembreDialog = AjouterMembreDialog(this) { nom, prenom, appogee ->
            if (nom.isNotBlank() && prenom.isNotBlank() && appogee.isNotBlank()) {
                membres.add(mapOf("nom" to nom, "prenom" to prenom, "appogee" to appogee))
                afficherMembres()
            } else {
                Toast.makeText(this, "Tous les champs du membre doivent être remplis.", Toast.LENGTH_SHORT).show()
            }
        }

        ajouterMembreDialog.afficherDialog()
    }



    private fun afficherMembres() {
        val membresText = membres.joinToString(separator = "\n\n") { membre ->
            "\t${membre["prenom"]} ${membre["nom"]} - \tAppogée: ${membre["appogee"]}"
        }
        binding.tvListeMembres.text = membresText
    }

    private fun creerGroupe(nomGroupe: String, nomProjet: String, dateDebut: String, dateFin: String) {
        val database = FirebaseDatabase.getInstance().getReference("Groupes")
        val groupeId = database.push().key ?: return

        val groupeData = mapOf(
            "nomGroupe" to nomGroupe,
            "nomProjet" to nomProjet,
            "dateDebut" to dateDebut,
            "dateFin" to dateFin,
            "membres" to membres
        )

        database.child(groupeId).setValue(groupeData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Groupe créé avec succès !", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, DetailDeGroupeActivity::class.java)
                intent.putExtra("groupeId", groupeId)
                startActivity(intent)

                finish()
            } else {
                Toast.makeText(this, "Erreur lors de la création du groupe: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
