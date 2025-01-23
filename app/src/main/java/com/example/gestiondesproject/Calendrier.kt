package com.example.gestiondesproject

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Calendrier : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var monthYearText: TextView
    private lateinit var legendLayout: LinearLayout
    private var currentCalendar: Calendar = Calendar.getInstance()
    private lateinit var groupeId: String
    private lateinit var status: String
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userDatabase: DatabaseReference

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("Groupes")
    private val groupNames: MutableList<String> = mutableListOf()
    private val groupColors: List<Int> = listOf(
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
        setContentView(R.layout.interface_calendrier)

        calendarGrid = findViewById(R.id.calendarGrid)
        monthYearText = findViewById(R.id.monthYearText)
        legendLayout = findViewById(R.id.legendLayout)
        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")

        // Récupérer l'ID du groupe et son nom depuis l'intent
        groupeId = intent.getStringExtra("groupeId") ?: "groupe123"
        status = intent.getStringExtra("status")?:"no status"


        findViewById<ImageButton>(R.id.prevMonthButton).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            setupCalendar()
        }

        findViewById<ImageButton>(R.id.nextMonthButton).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            setupCalendar()
        }

        fetchGroupsFromFirebase()
        setupCalendar()
        val backButton: ImageButton = findViewById(R.id.backButton)

        // Définir le comportement du bouton
        backButton.setOnClickListener {
            finish() // Retourner à l'activité précédente
        }

    }
    private fun setupCalendar() {
        // Vider la grille du calendrier
        calendarGrid.removeAllViews()

        // Afficher le mois et l'année actuels
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = dateFormat.format(currentCalendar.time)

        // Obtenir le nombre de jours dans le mois actuel
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK) -3

        // Obtenir les jours du mois précédent
        val previousMonth = Calendar.getInstance().apply {
            time = currentCalendar.time
            add(Calendar.MONTH, -1)
        }
        val daysInPreviousMonth = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Ajouter les jours du mois précédent, en gris
        for (i in firstDayOfWeek downTo 1) {
            val dayView = createDayView(daysInPreviousMonth - i + 1)
            dayView.alpha = 0.3f  // Griser les jours du mois précédent
            calendarGrid.addView(dayView)
        }

        // Ajouter les jours du mois actuel
        for (day in 1..daysInMonth) {
            val dayView = createDayView(day)

            if (isToday(day)) {
                // Ajouter un cercle autour de la date d'aujourd'hui
                val dayText = dayView.findViewById<TextView>(R.id.dayText)
                dayText.setBackgroundResource(R.drawable.circle_today)  // Utiliser un drawable pour entourer
            }

            dayView.setOnClickListener { showMenuDialog(day) }
            calendarGrid.addView(dayView)

            // Charger les événements pour ce jour depuis Firebase
            loadEventForDay(day)
        }

        // Ajouter les jours du mois suivant, en gris
        val remainingCells = 42 - (firstDayOfWeek + daysInMonth)
        for (i in 1..remainingCells) {
            val dayView = createDayView(i)
            dayView.alpha = 0.3f  // Griser les jours du mois suivant
            calendarGrid.addView(dayView)
        }

        // S'assurer que la grille ait toujours 42 cellules (6 lignes x 7 colonnes)
        val totalDays = firstDayOfWeek + daysInMonth + remainingCells
        val emptyCells = 42 - totalDays
        for (i in 1..emptyCells) {
            val dayView = createDayView(i)
            dayView.alpha = 0.3f  // Griser les jours vides
            calendarGrid.addView(dayView)
        }
    }



    private fun createDayView(day: Int): View {
        val dayView = LayoutInflater.from(this).inflate(R.layout.calendar_day_item, calendarGrid, false)
        val dayText = dayView.findViewById<TextView>(R.id.dayText)
        val eventText = dayView.findViewById<TextView>(R.id.eventText) // Ajoutez ce TextView dans votre layout
        dayText.text = day.toString()
        eventText.text = "" // Assurez-vous qu'il soit vide par défaut

        return dayView
    }

    private fun showMenuDialog(day: Int) {
        val options = arrayOf("Ajouter", "Modifier", "Supprimer")

        // Créer un AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEventDialog(day)
                    1 -> modifyEvent(day) // Modify event logic
                    2 -> deleteEvent(day)
                }
            }
            .create()

        // Appliquer la police Roboto et la taille de texte aux éléments de l'AlertDialog
        dialog.setOnShowListener {
            val listView = dialog.listView
            val robotoFont = ResourcesCompat.getFont(this, R.font.roboto_regular) // Charger la police Roboto

            for (i in 0 until listView.count) {
                val textView = listView.getChildAt(i) as TextView
                textView.typeface = robotoFont // Appliquer la police
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f) // Définir la taille du texte
            }
        }

        dialog.show()
    }



    private fun showEventDialog(day: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event, null)
        val eventEditText: EditText = dialogView.findViewById(R.id.eventNameEditText)
        val colorSpinner: Spinner = dialogView.findViewById(R.id.colorSpinner)
        val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)

        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupNames)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = colorAdapter



        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCustomTitle(createCustomTitle("Ajouter un événement"))
            .create()

        confirmButton.setOnClickListener {
            val eventName = eventEditText.text.toString()
            val selectedGroupIndex = colorSpinner.selectedItemPosition
            val eventColor = ContextCompat.getColor(this, groupColors[selectedGroupIndex])
            addEvent(day, eventName, eventColor)
            saveEventToFirebase(day, eventName, eventColor)  // Save event to Firebase
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addEvent(day: Int, eventName: String, color: Int) {
        val dayIndex = day + currentCalendar.get(Calendar.DAY_OF_WEEK) - 2
        val dayView = calendarGrid.getChildAt(dayIndex)
        val dayText = dayView.findViewById<TextView>(R.id.dayText)
        val eventText = dayView.findViewById<TextView>(R.id.eventText) // Récupérez le TextView de l'événement
        dayText.text = "$day"
        eventText.setTextColor(color) // Appliquez la couleur au texte de l'événement
        eventText.text = eventName // Affiche immédiatement l'événement ajouté
        eventText.visibility = View.VISIBLE // Rendre l'événement visible
    }

    private fun modifyEvent(day: Int) {
        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)
        val eventRef = database.child("Groupes").child("groupeId1").child("événements").child(eventDate).child(day.toString())

        eventRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val eventName = snapshot.child("nom").getValue(String::class.java)
                    val colorString = snapshot.child("couleur").getValue(String::class.java)
                    val currentColor = Color.parseColor(colorString)

                    // Afficher le dialogue pour modifier l'événement
                    val dialogView = LayoutInflater.from(this@Calendrier).inflate(R.layout.dialog_event, null)
                    val eventEditText: EditText = dialogView.findViewById(R.id.eventNameEditText)
                    val colorSpinner: Spinner = dialogView.findViewById(R.id.colorSpinner)
                    val confirmButton: Button = dialogView.findViewById(R.id.confirmButton)

                    // Préremplir le nom de l'événement et la couleur actuelle
                    eventEditText.setText(eventName)
                    val currentGroupIndex = groupColors.indexOfFirst { ContextCompat.getColor(this@Calendrier, it) == currentColor }
                    colorSpinner.setSelection(currentGroupIndex)

                    // Adapter pour le spinner des groupes
                    val colorAdapter = ArrayAdapter(this@Calendrier, android.R.layout.simple_spinner_item, groupNames)
                    colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    colorSpinner.adapter = colorAdapter


                    val dialog = AlertDialog.Builder(this@Calendrier)
                        .setView(dialogView)
                        .setCustomTitle(createCustomTitle("Modifier un événement"))
                        .create()

                    confirmButton.setOnClickListener {
                        val updatedEventName = eventEditText.text.toString()
                        val selectedGroupIndex = colorSpinner.selectedItemPosition
                        val updatedEventColor = ContextCompat.getColor(this@Calendrier, groupColors[selectedGroupIndex])

                        // Mettre à jour l'événement dans Firebase
                        updateEventInFirebase(day, updatedEventName, updatedEventColor)

                        // Mettre à jour l'affichage dans l'interface immédiatement après la modification
                        updateEventInCalendar(day, updatedEventName, updatedEventColor)
                        dialog.dismiss()
                    }

                    dialog.show()
                } else {
                    Toast.makeText(this@Calendrier, "Aucun événement trouvé pour ce jour", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Calendrier, "Erreur : ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateEventInFirebase(day: Int, updatedEventName: String, updatedEventColor: Int) {
        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)
        val eventRef = database.child("Groupes").child("groupeId1").child("événements").child(eventDate).child(day.toString())

        val updatedEvent = hashMapOf(
            "nom" to updatedEventName,
            "couleur" to String.format("#%06X", (0xFFFFFF and updatedEventColor)) // Convertir la couleur en hex
        )

        eventRef.setValue(updatedEvent)
            .addOnSuccessListener {
                Toast.makeText(this, "Événement modifié", Toast.LENGTH_SHORT).show()
                // Mettre à jour l'affichage du calendrier après modification
                updateEventInCalendar(day, updatedEventName, updatedEventColor)

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEventInCalendar(day: Int, eventName: String, eventColor: Int) {
        // Mettez à jour directement la vue du jour dans le calendrier
        val dayIndex = day + currentCalendar.get(Calendar.DAY_OF_WEEK) - 2
        val dayView = calendarGrid.getChildAt(dayIndex)
        val dayText = dayView.findViewById<TextView>(R.id.dayText)
        val eventText = dayView.findViewById<TextView>(R.id.eventText)

        // Mettre à jour l'événement modifié dans la vue du jour
        dayText.text = "$day"
        eventText.text = eventName
        eventText.setTextColor(eventColor)
        eventText.visibility = View.VISIBLE
    }

    private fun deleteEvent(day: Int) {
        // Récupérer la date de l'événement
        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)

        // Référence à l'événement dans Firebase
        val eventRef = database.child("Groupes").child("groupeId1").child("événements").child(eventDate).child(day.toString())

        // Supprimer l'événement de Firebase
        eventRef.removeValue()
            .addOnSuccessListener {
                // Une fois l'événement supprimé de Firebase, mettez à jour l'affichage du calendrier
                val dayIndex = day + currentCalendar.get(Calendar.DAY_OF_WEEK) - 2
                val dayView = calendarGrid.getChildAt(dayIndex)
                val eventText = dayView.findViewById<TextView>(R.id.eventText) // Récupérez le TextView de l'événement

                // Réinitialiser le texte de l'événement et le rendre invisible
                eventText.text = "" // Effacer le texte de l'événement
                eventText.visibility = View.INVISIBLE // Masquer le TextView de l'événement

                // Afficher un message de succès
                Toast.makeText(this, "Événement supprimé", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                // Si la suppression échoue, afficher un message d'erreur
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchGroupsFromFirebase() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupNames.clear()
                legendLayout.removeAllViews()

                // Créer un GridLayout pour la légende
                val gridLegend = GridLayout(this@Calendrier).apply {
                    columnCount = 3 // Fixer 3 colonnes
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                var colorIndex = 0
                for (groupSnapshot in snapshot.children) {
                    val groupName = groupSnapshot.child("nomGroupe").getValue(String::class.java)
                    if (!groupName.isNullOrEmpty()) {
                        groupNames.add(groupName)

                        // Créer un conteneur pour chaque groupe
                        val groupLegend = LinearLayout(this@Calendrier).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            layoutParams = GridLayout.LayoutParams().apply {
                                setMargins(16, 8, 16, 8) // Espacement entre les groupes
                            }
                        }

                        // Ajouter un point coloré
                        val colorDot = View(this@Calendrier).apply {
                            layoutParams = LinearLayout.LayoutParams(30, 30).apply {
                                setMargins(8, 0, 16, 0)
                            }
                            setBackgroundColor(ContextCompat.getColor(this@Calendrier, groupColors[colorIndex]))
                        }

                        // Ajouter le nom du groupe
                        val groupNameText = TextView(this@Calendrier).apply {
                            text = groupName
                            textSize = 16f
                            setTextColor(ContextCompat.getColor(this@Calendrier, R.color.black))
                        }

                        // Ajouter le point et le nom au conteneur
                        groupLegend.addView(colorDot)
                        groupLegend.addView(groupNameText)

                        // Ajouter le conteneur au GridLayout
                        gridLegend.addView(groupLegend)

                        colorIndex = (colorIndex + 1) % groupColors.size
                    }
                }

                // Ajouter le GridLayout à la légende principale
                legendLayout.addView(gridLegend)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Calendrier, "Erreur : ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Save event to Firebase
    private fun saveEventToFirebase(day: Int, eventName: String, color: Int) {
        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)
        val eventRef = database.child("Groupes").child("groupeId1").child("événements").child(eventDate).child(day.toString())

        val event = hashMapOf(
            "nom" to eventName,
            "couleur" to String.format("#%06X", (0xFFFFFF and color)) // Convertir la couleur en hex
        )

        eventRef.setValue(event)
            .addOnSuccessListener {
                Toast.makeText(this, "Événement ajouté", Toast.LENGTH_SHORT).show()
                loadEventForDay(day) // Charge l'événement du jour
            }

            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erreur : ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Load event for a specific day from Firebase
    private fun loadEventForDay(day: Int) {
        val eventDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time)
        val eventRef = database.child("Groupes").child("groupeId1").child("événements").child(eventDate).child(day.toString())

        eventRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val eventName = snapshot.child("nom").getValue(String::class.java)
                    val colorString = snapshot.child("couleur").getValue(String::class.java)
                    val eventColor = Color.parseColor(colorString)

                    if (!eventName.isNullOrEmpty()) {
                        val dayView = calendarGrid.getChildAt(day + currentCalendar.get(Calendar.DAY_OF_WEEK) - 2)
                        val dayText = dayView.findViewById<TextView>(R.id.dayText)
                        dayText.text = "$day"
                        val eventText = dayView.findViewById<TextView>(R.id.eventText)
                        eventText.text = eventName
                        eventText.setTextColor(eventColor)
                        eventText.visibility = View.VISIBLE // S'assurer que l'événement est visible

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Calendrier, "Erreur : ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun isToday(day: Int): Boolean {
        val todayCalendar = Calendar.getInstance()
        return currentCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                day == todayCalendar.get(Calendar.DAY_OF_MONTH)
    }
    private fun createCustomTitle(titleText: String): TextView {
        val titleView = TextView(this)
        titleView.text = titleText
        titleView.textSize = 20f // Taille de texte
        titleView.setTextColor(ContextCompat.getColor(this, R.color.app_color)) // Remplacez par votre couleur
        titleView.gravity = Gravity.CENTER // Centrer horizontalement
        titleView.setPadding(40, 40, 40, 20) // Ajouter du padding
        titleView.setTypeface(null, Typeface.BOLD) // Style gras (optionnel)
        titleView.setTypeface(ResourcesCompat.getFont(this, R.font.roboto_regular)) // Appliquer la police personnalisée
        return titleView
    }



}