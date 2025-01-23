package com.example.gestiondesproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import javax.net.ssl.SSLEngineResult.Status

class MenuGroupeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userDatabase: DatabaseReference
    private lateinit var groupeId: String
    private lateinit var groupeNom: String
    private lateinit var status: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_groupe)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")

        // Récupérer l'ID du groupe et son nom depuis l'intent
        groupeId = intent.getStringExtra("groupeId") ?: "groupe123"
        groupeNom = intent.getStringExtra("nomGroupe") ?: "Nom du groupe"
        status = intent.getStringExtra("status")?:"no status"

        // Définir le titre de la barre d'outils avec le nom du groupe
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        // Vérifier le rôle de l'utilisateur et ajuster le titre de la Toolbar

        // Configuration du toggle pour le drawer
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Afficher les informations de l'utilisateur connecté
        displayUserInfo()

        // Gestion de la navigation dans le menu
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, Interface2::class.java)
                    startActivity(intent)
                }
                R.id.nav_discussion -> {
                    Toast.makeText(this, "Discussion sélectionnée", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_taches -> {
                    val intent = Intent(this, Taches::class.java)
                    intent.putExtra("groupeId", groupeId)
                    intent.putExtra("status", status) // Ajoutez le status ici
                    startActivity(intent)
                }
                R.id.nav_avancement -> {
                    val intent = Intent(this, EtatAvancementActivity::class.java)
                    intent.putExtra("groupeId", groupeId)
                    startActivity(intent)

                    Toast.makeText(this, "État d'avancement sélectionné", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_detail_groupe -> {
                    val intent = Intent(this, DetailDeGroupeActivity::class.java)
                    intent.putExtra("groupeId", groupeId)
                    startActivity(intent)
                }
                R.id.nav_calendrie -> {
                    val intent = Intent(this, Calendrier::class.java)
                    intent.putExtra("groupeId", groupeId)
                    startActivity(intent)
                }
                R.id.nav_deconnexion -> {
                    FirebaseAuth.getInstance().signOut() // Déconnexion de Firebase
                    Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                    // Retourner à l'écran de connexion
                    val intent = Intent(this, Connexion::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()

                }

            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }


    // Méthode pour afficher les informations utilisateur dans le menu
    private fun displayUserInfo() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val userId = user.uid
            userDatabase.child(userId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val prenom = snapshot.child("prenom").value.toString()
                    val nom = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val status=snapshot.child("status").value.toString()

                    // Affichage des informations dans l'entête du menu
                    val headerView = navigationView.getHeaderView(0)
                    val prenomTextView = headerView.findViewById<TextView>(R.id.textViewPrenomUtilisateur)
                    val nomTextView = headerView.findViewById<TextView>(R.id.textViewNomUtilisateur)
                    val emailTextView = headerView.findViewById<TextView>(R.id.textViewEmail)
                    val profileImageView = headerView.findViewById<ImageView>(R.id.profileImageView) // Ajoutez cet ID à votre XML

                    prenomTextView.text = prenom
                    nomTextView.text = nom
                    emailTextView.text = email
                    if (status == "Professeur") {
                        profileImageView.setImageResource(R.drawable.baseline_home_repair_service_24) // Icône pour professeur
                        supportActionBar?.title = groupeNom
                        val navMenu = navigationView.menu
                        navMenu.findItem(R.id.nav_calendrie).isVisible=false
                    } else if (status == "Étudiant") {
                        profileImageView.setImageResource(R.drawable.baseline_school_24) // Icône pour étudiant
                        supportActionBar?.title = "FinalSprint"

                        // Ou un titre vide ou un message approprié
                        val navMenu = navigationView.menu
                        navMenu.findItem(R.id.nav_home).isVisible = false
                        navMenu.findItem(R.id.nav_calendrie).isVisible=true
                    }
                } else {
                    Toast.makeText(this, "Aucune donnée utilisateur trouvée.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Erreur de récupération des données utilisateur.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Utilisateur non connecté.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gérer la fermeture du Drawer avec le bouton "Retour"
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}