package com.example.gestiondesproject

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.database.*
import Group
import android.annotation.SuppressLint
import android.widget.ImageButton

class EtatAvancementActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var projectTitleTextView: TextView
    private val database = FirebaseDatabase.getInstance().getReference("Groupes")
    private val taskPercentages = mutableMapOf<String, Float>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_etat_avancement)

        // Récupérer les vues
        pieChart = findViewById(R.id.pieChart)
        projectTitleTextView = findViewById(R.id.projectTitle)

        // Récupérer l'ID du groupe à partir de l'intent
        val groupId = intent.getStringExtra("groupeId") ?: ""

        if (groupId.isNotEmpty()) {
            // Charger les données du groupe pour récupérer le nom du projet
            loadGroupData(groupId)
        }
        val backButton: ImageButton = findViewById(R.id.backButton6)

        // Définir le comportement du bouton
        backButton.setOnClickListener {
            finish() // Retourner à l'activité précédente
        }
    }

    private fun loadGroupData(groupId: String) {
        val groupRef = database.child(groupId)

        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Convertir les données Firebase en un objet Group
                val group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    // Mettre à jour le titre du projet
                    projectTitleTextView.text = group.nomProjet

                    // Charger les données des tâches une fois le groupe récupéré
                    loadTaskData(groupId)
                } else {
                    Toast.makeText(this@EtatAvancementActivity, "Données du groupe introuvables", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EtatAvancementActivity, "Erreur de chargement des données du groupe", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadTaskData(groupId: String) {
        val tasksRef = database.child(groupId).child("Taches")

        tasksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskEntries = mutableListOf<Pair<String, String>>() // Liste des paires (nom de tâche, statut)

                for (taskSnapshot in snapshot.children) {
                    val status = taskSnapshot.child("status").getValue(String::class.java) ?: ""
                    val taskName = taskSnapshot.child("title").getValue(String::class.java) ?: "Nom inconnu"
                    taskEntries.add(Pair(taskName, status))
                }

                displayPieChart(taskEntries)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EtatAvancementActivity, "Erreur de chargement des données des tâches", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayPieChart(taskEntries: List<Pair<String, String>>) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        var totalProgress = 0f
        val totalTasks = taskEntries.size

        for ((taskName, status) in taskEntries) {
            val taskPercentage = when (status) {
                "accepted" -> 100f / totalTasks
                "modified" -> (100f / totalTasks) / 2
                "rejected" -> 0f
                else -> 0f
            }

            totalProgress += taskPercentage
            taskPercentages[taskName] = taskPercentage

            entries.add(PieEntry(1f, taskName))
            val color = when (status) {
                "accepted" -> android.graphics.Color.GREEN
                "modified" -> android.graphics.Color.rgb(255, 165, 0)
                "rejected" -> android.graphics.Color.RED
                else -> android.graphics.Color.GRAY
            }
            colors.add(color)
        }

        val dataSet = PieDataSet(entries, "Tâches")
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        val layoutParams = pieChart.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(0, 150, 0, 0) // Ajouter une marge supérieure de 50dp
        pieChart.layoutParams = layoutParams


        val progressText = "Avancement :\n${String.format("%.1f", totalProgress)}%"
        pieChart.centerText = progressText
        pieChart.setCenterTextColor(android.graphics.Color.BLUE)
        pieChart.setCenterTextSize(16f)

        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM // Place la légende en bas
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER // Centre la légende horizontalement
        legend.orientation = Legend.LegendOrientation.HORIZONTAL // Affiche les éléments horizontalement
        legend.setDrawInside(false) // Place la légende en dehors du graphique
        legend.textSize = 12f
        legend.textColor = android.graphics.Color.BLACK
        legend.formToTextSpace = 8f
        legend.xEntrySpace = 90f // Espace entre les éléments de la légende (réduit à la moitié si vous voulez 2 éléments par ligne)
        legend.yEntrySpace = 10f

// Diviser la légende en deux éléments par ligne
        val legendEntries = listOf(
            LegendEntry("Accepté", Legend.LegendForm.CIRCLE, 10f, 2f, null, android.graphics.Color.GREEN),
            LegendEntry("Modifié", Legend.LegendForm.CIRCLE, 10f, 2f, null, android.graphics.Color.rgb(255, 165, 0)),
            LegendEntry("Rejeté", Legend.LegendForm.CIRCLE, 10f, 2f, null, android.graphics.Color.RED),
            LegendEntry("Non évalué", Legend.LegendForm.CIRCLE, 10f, 2f, null, android.graphics.Color.GRAY)
        )
        legend.setCustom(legendEntries)
        legend.isWordWrapEnabled = true


        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
                val taskName = (e as? PieEntry)?.label ?: return
                val taskPercentage = taskPercentages[taskName] ?: 0f
                pieChart.highlightValues(arrayOf(h))
                pieChart.centerText = "Tâche : $taskName\n${String.format("%.1f", taskPercentage)}%"
                pieChart.invalidate()
            }

            override fun onNothingSelected() {
                pieChart.highlightValues(null)
                val progressText = "Avancement :\n${String.format("%.1f", totalProgress)}%"
                pieChart.centerText = progressText
                pieChart.invalidate()
            }
        })

        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
