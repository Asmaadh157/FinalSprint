package com.example.gestiondesproject

import android.app.ActionBar
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.firebase.database.*

class MesGroupesActivity : AppCompatActivity() {

    private lateinit var linearLayoutGroupes: LinearLayout
    private val database = FirebaseDatabase.getInstance().getReference("Groupes")

    // Liste des couleurs
    private val couleurs = listOf(
        R.color.groupe_color_1,
        R.color.groupe_color_2,
        R.color.groupe_color_3,
        R.color.groupe_color_4,
        R.color.groupe_color_5,
        R.color.groupe_color_6,
        R.color.groupe_color_7,
        R.color.groupe_color_8,
        R.color.groupe_color_9,
        R.color.groupe_color_10
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mes_groupes)

        linearLayoutGroupes = findViewById(R.id.linearLayoutGroupes)

        // Appel à la méthode pour récupérer les groupes
        recupererGroupes()

        // Configurer le bouton pour ajouter un groupe
        val btnAjouterGroupe: Button = findViewById(R.id.btnAjouterGroupe)
        btnAjouterGroupe.setOnClickListener {
            val intent = Intent(this, CreerGroupeActivity::class.java)
            startActivity(intent)
        }

        // Configurer le bouton de retour
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    // Fonction pour récupérer les groupes depuis Firebase
    private fun recupererGroupes() {
        // Ajout d'un écouteur pour récupérer les données en temps réel
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Effacer les anciens groupes de l'interface avant d'ajouter les nouveaux
                linearLayoutGroupes.removeAllViews()

                // Vérifier si des groupes existent
                if (snapshot.exists()) {
                    var indexColor = 0 // Compteur pour les couleurs

                    // Itérer sur tous les groupes disponibles
                    for (groupeSnapshot in snapshot.children) {
                        val groupeId = groupeSnapshot.key ?: continue
                        val nomGroupe = groupeSnapshot.child("nomGroupe").getValue(String::class.java)

                        // Si le nom du groupe est valide
                        if (!nomGroupe.isNullOrEmpty()) {
                            val btnGroupe = Button(this@MesGroupesActivity).apply {
                                text = nomGroupe
                                setBackgroundColor(ContextCompat.getColor(context, R.color.app_color)) // Fond blanc
                                setTextColor(ContextCompat.getColor(context,R.color.black)) // Texte noir
                                textSize = 16f
                                background = ContextCompat.getDrawable(context, R.drawable.border_app_color) // Application du fond avec le fichier drawable
                                typeface = ResourcesCompat.getFont(context, R.font.roboto_regular)

                                // Obtenez le VectorDrawable et appliquez une couleur
                                val drawable: Drawable? = resources.getDrawable(R.drawable.baseline_groups_24, null)
                                drawable?.setBounds(100, 100, 100, 100)  // Modifier cette taille selon vos besoins
                                // Appliquer une couleur dynamique en fonction de l'index
                                val iconColor = resources.getColor(couleurs[indexColor], null)

                                // Modifier la couleur du drawable
                                DrawableCompat.setTint(drawable!!, iconColor)
                                // Ajouter l'icône à gauche du texte et ajouter un décalage
                                setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

                                // Ajouter du padding pour déplacer légèrement l'icône vers la droite
                                setPadding(100, 0, 0, 0)  // Ajustez la valeur du padding en fonction de vos besoins
                                // Ajouter une marge entre les boutons
                                val params = ActionBar.LayoutParams(
                                    ActionBar.LayoutParams.MATCH_PARENT,
                                    ActionBar.LayoutParams.WRAP_CONTENT
                                )
                                params.setMargins(0, 20, 0, 0) // Marge de 16dp en haut (espace entre les boutons)
                                layoutParams = params

                                setOnClickListener {
                                    // Rediriger vers l'activité du menu du groupe
                                    val intent = Intent(this@MesGroupesActivity, MenuGroupeActivity::class.java)
                                    intent.putExtra("groupeId", groupeId)
                                    intent.putExtra("nomGroupe", nomGroupe)
                                    startActivity(intent)
                                }
                                setOnLongClickListener {
                                    afficherMenuContextuel(this, groupeId)
                                    true
                                }
                            }

                            // Ajouter le bouton à l'interface utilisateur
                            linearLayoutGroupes.addView(btnGroupe)

                            // Incrémenter l'index pour appliquer la couleur suivante, et réinitialiser si nécessaire
                            indexColor = (indexColor + 1) % couleurs.size
                        }
                    }
                } else {
                    // Si aucun groupe n'est trouvé, afficher un message
                    Toast.makeText(this@MesGroupesActivity, "Aucun groupe disponible.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // En cas d'erreur lors de la récupération des données, afficher un message d'erreur
                Toast.makeText(this@MesGroupesActivity, "Erreur : ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun afficherMenuContextuel(anchor: Button, groupeId: String) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_groupe_contextuel, popupMenu.menu)


        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_supprimer -> {
                    supprimerGroupe(groupeId)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // Supprimer un groupe
    private fun supprimerGroupe(groupeId: String) {
        database.child(groupeId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Groupe supprimé avec succès.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erreur lors de la suppression.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
