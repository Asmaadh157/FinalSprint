package com.example.gestiondesproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gestiondesproject.databinding.InscrireBinding
import com.google.firebase.database.*

class DetailDeGroupeActivity : AppCompatActivity() {

    private lateinit var groupeId: String
    private lateinit var database: DatabaseReference
    private lateinit var tvNomGroupe: TextView
    private lateinit var tvNomProjet: TextView
    private lateinit var tvDateDebut: TextView
    private lateinit var tvDateFin: TextView
    private lateinit var tvMembres: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_groupe)  // Assurez-vous que ce fichier XML existe dans res/layout

        // Initialisation des vues
        tvNomGroupe = findViewById(R.id.tvNomGroupe)
        tvNomProjet = findViewById(R.id.tvNomProjet)
        tvDateDebut = findViewById(R.id.tvDateDebut)
        tvDateFin = findViewById(R.id.tvDateFin)
        tvMembres = findViewById(R.id.tvMembres)

        // Récupérer l'ID du groupe passé dans l'intent
        groupeId = intent.getStringExtra("groupeId") ?: ""

        if (groupeId.isNotEmpty()) {
            // Initialisation de Firebase
            database = FirebaseDatabase.getInstance().getReference("Groupes").child(groupeId)
            recupererDetailsGroupe()
        } else {
            Toast.makeText(this, "ID du groupe non valide.", Toast.LENGTH_SHORT).show()
            finish()
        }
        val btnRetour: ImageButton = findViewById(R.id.BackButton1)
        btnRetour.setOnClickListener {
            finish() // Ferme l'activité en cours et retourne à l'interface précédente
        }
    }

    private fun recupererDetailsGroupe() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val nomGroupe = snapshot.child("nomGroupe").value.toString()
                    val nomProjet = snapshot.child("nomProjet").value.toString()
                    val dateDebut = snapshot.child("dateDebut").value.toString()
                    val dateFin = snapshot.child("dateFin").value.toString()
                    val membresSnapshot = snapshot.child("membres")

                    tvNomGroupe.text = "\t$nomGroupe"
                    tvNomProjet.text = "\t$nomProjet"
                    tvDateDebut.text = "\t$dateDebut"
                    tvDateFin.text =   "\t$dateFin"

                    // Afficher les membres du groupe
                    val membres = mutableListOf<String>()
                    for (membreSnapshot in membresSnapshot.children) {
                        val nomMembre = membreSnapshot.child("nom").value.toString()
                        val prenomMembre = membreSnapshot.child("prenom").value.toString()
                        val appogeeMembre = membreSnapshot.child("appogee").value.toString()
                        membres.add("-$prenomMembre $nomMembre ")
                    }
                    tvMembres.text = "${membres.joinToString("\n")}"
                } else {
                    Toast.makeText(this@DetailDeGroupeActivity, "Aucun détail pour ce groupe.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailDeGroupeActivity, "Erreur: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    override fun onBackPressed() {
        // Affiche un message ou effectue une action personnalisée
        Toast.makeText(this, "Retour en arrière détecté !", Toast.LENGTH_SHORT).show()

        // Revenir à l'activité précédente
        super.onBackPressed()
    }
}
