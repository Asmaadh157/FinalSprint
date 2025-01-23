package com.example.gestiondesproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class Interface2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.interface2) // Assurez-vous que l'ID du layout est correct

        // Liaison des boutons définis dans le fichier XML
        val btnCreerGroupe = findViewById<CardView>(R.id.btnCreerGroupe)
        val btnMesGroupes = findViewById<CardView>(R.id.btnMesGroupes)
        val btnCalendrier = findViewById<CardView>(R.id.btnCalendrier)
        val btnDeconnexion = findViewById<CardView>(R.id.btnDeconnexion)

        // Gestion des clics sur les boutons

        // Action pour "Créer un groupe"
        btnCreerGroupe.setOnClickListener {
            Toast.makeText(this, "Créer un groupe sélectionné", Toast.LENGTH_SHORT).show()
            // Naviguer vers une activité dédiée à la création de groupes
            val intent = Intent(this, CreerGroupeActivity::class.java) // Remplacez par l'activité réelle
            startActivity(intent)
        }

        // Action pour "Mes groupes"
        btnMesGroupes.setOnClickListener {
            Toast.makeText(this, "Afficher mes groupes", Toast.LENGTH_SHORT).show()
            // Naviguer vers une activité qui affiche les groupes
            val intent = Intent(this, MesGroupesActivity::class.java) // Remplacez par l'activité réelle
            startActivity(intent)
        }

        // Action pour "Calendrier"
        btnCalendrier.setOnClickListener {
            Toast.makeText(this, "Accéder au calendrier", Toast.LENGTH_SHORT).show()
            // Naviguer vers une activité de calendrier
            val intent = Intent(this, Calendrier::class.java) // Remplacez par l'activité réelle
            startActivity(intent)
        }

        // Action pour "Déconnexion"
        btnDeconnexion.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Déconnexion de Firebase
            Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
            // Retourner à l'écran de connexion
            val intent = Intent(this, Connexion::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK // Effacer la pile d'activités
            startActivity(intent)
            finish() // Fermer l'activité actuelle
        }
    }
}
