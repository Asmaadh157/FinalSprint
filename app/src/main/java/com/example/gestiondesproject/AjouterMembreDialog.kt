package com.example.gestiondesproject

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AjouterMembreDialog(
    private val activity: AppCompatActivity,
    val onMembreAjoute: (nom: String, prenom: String, appogee: String) -> Unit
) {
    private lateinit var dialog: Dialog

    fun afficherDialog() {
        dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ajouter_member)
        dialog.setCancelable(true)

        // Récupérer les champs de l'interface
        val etNomMembre = dialog.findViewById<EditText>(R.id.etNomMembre)
        val etPrenomMembre = dialog.findViewById<EditText>(R.id.etPrenomMembre)
        val etAppogeeMembre = dialog.findViewById<EditText>(R.id.etAppogeeMembre)
        val btnAjouter = dialog.findViewById<Button>(R.id.btnAjouter)

        // Action du bouton Ajouter
        btnAjouter.setOnClickListener {
            val nom = etNomMembre.text.toString().trim()
            val prenom = etPrenomMembre.text.toString().trim()
            val appogee = etAppogeeMembre.text.toString().trim()

            // Validation des champs
            if (nom.isEmpty() || prenom.isEmpty() || appogee.isEmpty()) {
                Toast.makeText(activity, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!appogee.matches(Regex("\\d+"))) {
                Toast.makeText(activity, "Le numéro d'appogée doit contenir uniquement des chiffres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Appeler la fonction de rappel pour passer les données
            onMembreAjoute(nom, prenom, appogee)
            dialog.dismiss() // Fermer le dialogue après ajout
        }

        dialog.show() // Afficher le dialogue
    }
}
